package com.example.sms

import com.example.data.model.BankCardEntity
import com.example.data.model.TransactionCategory
import com.example.util.CurrencyUtils

data class ParsedBankSms(
    val isValid: Boolean,
    val bankName: String,
    val accountNumber: String,
    val type: String, // "EXPENSE" or "INCOME"
    val amountRials: Long,
    val balanceRials: Long,
    val dateString: String,
    val rawSms: String,
    val sender: String,
    val suggestedCategory: TransactionCategory
)

object BankSmsParser {

    /**
     * Normalizes Persian characters, Arabic kaf/yeh, and digits for robust parsing
     */
    fun normalizePersianText(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            when {
                ch in '۰'..'۹' -> sb.append(ch - '۰')
                ch in '٠'..'٩' -> sb.append(ch - '٠')
                ch == 'ي' -> sb.append('ی')
                ch == 'ك' -> sb.append('ک')
                ch == '،' -> sb.append(',')
                ch == '؛' -> sb.append(';')
                ch == 'ة' -> sb.append('ه')
                ch == 'ؤ' -> sb.append('و')
                ch == 'إ' || ch == 'أ' || ch == 'آ' -> sb.append('ا')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Cleans phone numbers by stripping Iranian international prefix, leading zeroes and separators
     */
    fun cleanPhoneNumber(phone: String): String {
        var p = phone.trim().replace("+98", "").replace(" ", "").replace("-", "")
        if (p.startsWith("0098")) p = p.substring(4)
        if (p.startsWith("98") && p.length > 8) p = p.substring(2)
        if (p.startsWith("0") && p.length > 5) p = p.substring(1)
        return p.lowercase()
    }

    /**
     * Determines if an SMS is strictly an OTP, password verification, or purely informational message.
     * If the message contains financial transaction signals (withdrawal, deposit, purchase, signed amount, balance),
     * it is NOT treated as an OTP.
     */
    fun isOtpOrInformational(body: String): Boolean {
        val b = normalizePersianText(body).lowercase()

        // If it has strong transaction indicators, it is NOT an OTP even if it says "محرمانه"
        val hasTransactionSignals = (b.contains("برداشت") || b.contains("واریز") ||
                b.contains("خرید") || b.contains("مانده") || b.contains("موجودی") ||
                Regex("""[0-9,]{3,}\s*[+-]""").containsMatchIn(b) ||
                Regex("""[+-]\s*[0-9,]{3,}""").containsMatchIn(b))

        if (hasTransactionSignals) {
            return false
        }

        val otpKeywords = listOf(
            "رمز پویا", "رمز یکبار", "یکبار مصرف", "رمز موقت", "رمز دوم",
            "کد تایید", "کد تائید", "کد فعالسازی", "کد اعتبارسنجی",
            "رمز انتقال", "خوش آمدید", "به سامانه همراه", "ورود به سامانه",
            "ورود به همراه بانک", "ورود به اینترنت بانک", "رمز ورود"
        )
        return otpKeywords.any { b.contains(it) }
    }

    /**
     * Determines if an incoming SMS looks like an Iranian bank SMS
     */
    fun isBankSms(sender: String, body: String, configuredCards: List<BankCardEntity> = emptyList()): Boolean {
        if (isOtpOrInformational(body)) return false

        val cleanSender = cleanPhoneNumber(sender)
        val rawSender = sender.trim().lowercase()
        val cleanBody = normalizePersianText(body).lowercase()

        // 1. Matches any configured card's contact or account
        for (card in configuredCards) {
            if (card.contactPhone.isNotBlank()) {
                val cleanCardPhone = cleanPhoneNumber(card.contactPhone)
                if (cleanSender == cleanCardPhone || cleanSender.endsWith(cleanCardPhone) || cleanCardPhone.endsWith(cleanSender)) {
                    return true
                }
            }
            if (card.senderContact.isNotBlank()) {
                val contact = card.senderContact.trim().lowercase()
                if (rawSender.contains(contact) || contact.contains(rawSender)) {
                    return true
                }
            }
            if (card.accountNumber.isNotBlank() && cleanBody.contains(card.accountNumber)) {
                return true
            }
        }

        // 2. Known Iranian bank shortcodes and sender names
        val bankSenders = listOf(
            "20000", "2000", "200000", "mellat", "بانک ملت",
            "200060", "300060", "saderat", "صادرات", "b.saderat", "bsaderat",
            "20004000", "melli", "بانک ملی",
            "2000070", "tejarat", "تجارت",
            "10008888", "pasargad", "پاسارگاد",
            "3000", "saman", "سامان",
            "sepah", "سپه",
            "refah", "رفاه", "parsian", "پارسیان"
        )

        val senderMatches = bankSenders.any { 
            rawSender.contains(it) || cleanSender.contains(it) || it.contains(cleanSender)
        }

        // Bank transaction keywords in Persian
        val hasBankKeywords = (cleanBody.contains("برداشت") || cleanBody.contains("واریز") ||
                cleanBody.contains("خرید") || cleanBody.contains("مانده") ||
                cleanBody.contains("موجودی") || cleanBody.contains("حساب") ||
                cleanBody.contains("انتقال") || cleanBody.contains("کارمزد") ||
                cleanBody.contains("پایانه فروش") || cleanBody.contains("سرویس پیامک"))

        // Signed amount pattern with account/balance (Bank Saderat signature)
        val hasSignedAmountPattern = (Regex("""[0-9,]{3,}\s*[+-]""").containsMatchIn(cleanBody) ||
                Regex("""[+-]\s*[0-9,]{3,}""").containsMatchIn(cleanBody)) &&
                (cleanBody.contains("حساب") || cleanBody.contains("مانده"))

        return senderMatches || hasBankKeywords || hasSignedAmountPattern
    }

    /**
     * Detects bank name from sender, message content, or configured cards
     */
    fun detectBankName(
        sender: String,
        body: String,
        configuredCards: List<BankCardEntity> = emptyList()
    ): String {
        val s = sender.lowercase()
        val cleanSender = cleanPhoneNumber(sender)
        val b = normalizePersianText(body).lowercase()

        // 1. Direct match with configured cards
        for (card in configuredCards) {
            if (card.accountNumber.isNotBlank()) {
                val acc = card.accountNumber.trim()
                if (b.contains(acc) || Regex("""حساب\s*:?\s*$acc""").containsMatchIn(b)) {
                    return card.bankName
                }
            }
            if (card.contactPhone.isNotBlank()) {
                val cardPhone = cleanPhoneNumber(card.contactPhone)
                if (cleanSender == cardPhone || cleanSender.endsWith(cardPhone) || cardPhone.endsWith(cleanSender)) {
                    return card.bankName
                }
            }
            if (card.senderContact.isNotBlank()) {
                val contact = card.senderContact.trim().lowercase()
                if (s.contains(contact) || contact.contains(s) || b.contains(contact)) {
                    return card.bankName
                }
            }
        }

        // 2. Standard bank detection
        return when {
            s.contains("saderat") || s.contains("صادرات") || cleanSender.contains("200060") || cleanSender.contains("300060") ||
                    b.contains("صادرات") || b.contains("صادارت") ||
                    // Saderat unique SMS format signature (signed amount + account/balance)
                    ((Regex("""[0-9,]{3,}\s*[+-]""").containsMatchIn(b) || Regex("""[+-]\s*[0-9,]{3,}""").containsMatchIn(b)) &&
                            Regex("""حساب\s*:?\s*[0-9]+""").containsMatchIn(b) &&
                            Regex("""مانده\s*:?\s*[0-9]+""").containsMatchIn(b)) ||
                    (Regex("""\b[0-9]{4}\s*-\s*[0-9]{1,2}:[0-9]{1,2}\b""").containsMatchIn(b) && b.contains("مانده")) ||
                    (b.contains("پایانه فروش") && b.contains("مانده")) ||
                    (b.contains("سرویس پیامک") && b.contains("مانده")) -> "بانک صادرات"

            s.contains("20000") || cleanSender.contains("20000") || s.contains("mellat") || b.contains("ملت") -> "بانک ملت"
            s.contains("20004000") || cleanSender.contains("20004000") || s.contains("melli") || b.contains("ملی") -> "بانک ملی"
            s.contains("2000070") || cleanSender.contains("2000070") || s.contains("tejarat") || b.contains("تجارت") -> "بانک تجارت"
            s.contains("pasargad") || b.contains("پاسارگاد") -> "بانک پاسارگاد"
            s.contains("saman") || b.contains("سامان") -> "بانک سامان"
            s.contains("sepah") || b.contains("سپه") -> "بانک سپه"
            s.contains("parsian") || b.contains("پارسیان") -> "بانک پارسیان"
            s.contains("keshavarzi") || b.contains("کشاورزی") -> "بانک کشاورزی"
            s.contains("refah") || b.contains("رفاه") -> "بانک رفاه"
            b.contains("حساب") && (b.contains("برداشت") || b.contains("واریز")) && b.contains("مانده") -> "بانک ملت"
            else -> "بانک من"
        }
    }

    /**
     * Parses an Iranian bank SMS message
     */
    fun parse(
        body: String,
        sender: String = "",
        configuredCards: List<BankCardEntity> = emptyList()
    ): ParsedBankSms {
        // Exclude OTP and informational non-transactions immediately
        if (isOtpOrInformational(body)) {
            return ParsedBankSms(
                isValid = false,
                bankName = detectBankName(sender, body, configuredCards),
                accountNumber = "",
                type = "OTHER",
                amountRials = 0L,
                balanceRials = 0L,
                dateString = "",
                rawSms = body.trim(),
                sender = sender.trim(),
                suggestedCategory = TransactionCategory.OTHER
            )
        }

        val normalizedBody = normalizePersianText(body)
        val lines = normalizedBody.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var accountNumber = ""
        var type = "EXPENSE"
        var amountRials = 0L
        var balanceRials = 0L
        var dateString = ""

        // Line-by-line parsing
        for (line in lines) {
            // 1. Account number match (حساب:54321 or حساب: 54321 or حساب1234567890 or شماره کارت ...)
            if (accountNumber.isEmpty()) {
                val accRegex = Regex("""(?:حساب|کارت|شماره)\s*:?\s*([0-9*]{4,20})""")
                val accMatch = accRegex.find(line)
                if (accMatch != null) {
                    accountNumber = accMatch.groupValues[1]
                }
            }

            // 2. Saderat signed amount (e.g. "انتقال: 20,000,000+" or "كارمزد سرويس پيامك: 200,000-" or "پايانه فروش: 900,000-")
            if (amountRials == 0L) {
                val signedSuffixRegex = Regex("""([0-9,]{3,})\s*([+-])""")
                val signedPrefixRegex = Regex("""([+-])\s*([0-9,]{3,})""")
                val matchSuffix = signedSuffixRegex.find(line)
                val matchPrefix = signedPrefixRegex.find(line)
                if (matchSuffix != null) {
                    val sign = matchSuffix.groupValues[2]
                    type = if (sign == "+") "INCOME" else "EXPENSE"
                    amountRials = CurrencyUtils.parseAmount(matchSuffix.groupValues[1])
                } else if (matchPrefix != null) {
                    val sign = matchPrefix.groupValues[1]
                    type = if (sign == "+") "INCOME" else "EXPENSE"
                    amountRials = CurrencyUtils.parseAmount(matchPrefix.groupValues[2])
                }
            }

            // 3. Withdrawal / Expense (برداشت10,850,000 or خرید10,850,000 or مبلغ: 500,000-)
            if (amountRials == 0L) {
                val withdrawRegex = Regex("""(?:برداشت|خرید|کسر|بدهکار|انتقال از|پایانه فروش)\s*:?\s*([0-9,]+)""")
                val withdrawMatch = withdrawRegex.find(line)
                if (withdrawMatch != null) {
                    type = "EXPENSE"
                    amountRials = CurrencyUtils.parseAmount(withdrawMatch.groupValues[1])
                }
            }

            // 4. Deposit / Income (واریز10,850,000 or سود or بستانکار)
            if (amountRials == 0L) {
                val depositRegex = Regex("""(?:واریز|سود|بستانکار|انتقال به)\s*:?\s*([0-9,]+)""")
                val depositMatch = depositRegex.find(line)
                if (depositMatch != null) {
                    type = "INCOME"
                    amountRials = CurrencyUtils.parseAmount(depositMatch.groupValues[1])
                }
            }

            // 5. Generic Amount line (e.g. مبلغ: 500,000 or مبلغ 500,000)
            if (amountRials == 0L) {
                val amountRegex = Regex("""(?:مبلغ|وجه|هزینه)\s*:?\s*([0-9,]+)""")
                val amountMatch = amountRegex.find(line)
                if (amountMatch != null) {
                    amountRials = CurrencyUtils.parseAmount(amountMatch.groupValues[1])
                }
            }

            // 6. Balance (مانده98,765,432 or مانده:33,456,789 or مانده: 33,456,789 or موجودی)
            if (balanceRials == 0L) {
                val balanceRegex = Regex("""(?:مانده|موجودی)\s*:?\s*([0-9,]+)""")
                val balanceMatch = balanceRegex.find(line)
                if (balanceMatch != null) {
                    balanceRials = CurrencyUtils.parseAmount(balanceMatch.groupValues[1])
                }
            }

            // 7. Saderat date and time format (e.g. 0601 - 16:57 or 0612 - 13:36 or 0616 - 12:54 or 0620-14:10 or 0620 14:10)
            if (dateString.isEmpty()) {
                val saderatDateRegex = Regex("""\b([0-9]{4}\s*[-/ ]\s*[0-9]{1,2}:[0-9]{1,2})\b""")
                val saderatDateMatch = saderatDateRegex.find(line)
                if (saderatDateMatch != null) {
                    dateString = saderatDateMatch.groupValues[1]
                }
            }

            // 8. Standard Persian date and time (e.g. 05/06/21-22:46 or 1404/05/05 12:57 or 1403/06/20)
            if (dateString.isEmpty()) {
                val dateRegex = Regex("""([0-9]{2,4}[/-][0-9]{1,2}[/-][0-9]{1,2}(?:[- ][0-9]{1,2}:[0-9]{1,2})?)""")
                val dateMatch = dateRegex.find(line)
                if (dateMatch != null) {
                    dateString = dateMatch.groupValues[1]
                }
            }
        }

        // Full-text fallback if line parsing missed something
        if (amountRials == 0L) {
            val signedSuffixRegex = Regex("""([0-9,]{3,})\s*([+-])""")
            val matchSuffix = signedSuffixRegex.find(normalizedBody)
            if (matchSuffix != null) {
                type = if (matchSuffix.groupValues[2] == "+") "INCOME" else "EXPENSE"
                amountRials = CurrencyUtils.parseAmount(matchSuffix.groupValues[1])
            }
        }
        if (amountRials == 0L) {
            val signedPrefixRegex = Regex("""([+-])\s*([0-9,]{3,})""")
            val matchPrefix = signedPrefixRegex.find(normalizedBody)
            if (matchPrefix != null) {
                type = if (matchPrefix.groupValues[1] == "+") "INCOME" else "EXPENSE"
                amountRials = CurrencyUtils.parseAmount(matchPrefix.groupValues[2])
            }
        }
        if (amountRials == 0L) {
            val withdrawRegex = Regex("""(?:برداشت|خرید|کسر)\s*:?\s*([0-9,]+)""")
            withdrawRegex.find(normalizedBody)?.let {
                type = "EXPENSE"
                amountRials = CurrencyUtils.parseAmount(it.groupValues[1])
            }
        }
        if (amountRials == 0L) {
            val depositRegex = Regex("""(?:واریز|سود|بستانکار)\s*:?\s*([0-9,]+)""")
            depositRegex.find(normalizedBody)?.let {
                type = "INCOME"
                amountRials = CurrencyUtils.parseAmount(it.groupValues[1])
            }
        }
        if (balanceRials == 0L) {
            val balanceRegex = Regex("""(?:مانده|موجودی)\s*:?\s*([0-9,]+)""")
            balanceRegex.find(normalizedBody)?.let {
                balanceRials = CurrencyUtils.parseAmount(it.groupValues[1])
            }
        }
        if (accountNumber.isEmpty()) {
            val accRegex = Regex("""(?:حساب|کارت)\s*:?\s*([0-9*]{4,20})""")
            accRegex.find(normalizedBody)?.let {
                accountNumber = it.groupValues[1]
            }
        }
        if (dateString.isEmpty()) {
            val saderatDateRegex = Regex("""\b([0-9]{4}\s*[-/ ]\s*[0-9]{1,2}:[0-9]{1,2})\b""")
            saderatDateRegex.find(normalizedBody)?.let {
                dateString = it.groupValues[1]
            }
        }
        if (dateString.isEmpty()) {
            val dateRegex = Regex("""([0-9]{2,4}[/-][0-9]{1,2}[/-][0-9]{1,2}(?:[- ][0-9]{1,2}:[0-9]{1,2})?)""")
            dateRegex.find(normalizedBody)?.let {
                dateString = it.groupValues[1]
            }
        }

        var bankName = detectBankName(sender, body, configuredCards)

        // If card configuration matches account, sync bank name exactly with that card
        for (card in configuredCards) {
            if (card.accountNumber.isNotBlank() && (accountNumber == card.accountNumber || normalizedBody.contains(card.accountNumber))) {
                bankName = card.bankName
                if (accountNumber.isEmpty()) {
                    accountNumber = card.accountNumber
                }
                break
            }
        }

        val isIncome = type == "INCOME"
        val suggestedCategory = TransactionCategory.suggestCategory(body, isIncome)

        // Valid only if there is a real amount and account/balance, and not an OTP/info message
        val isValid = amountRials > 0L && (balanceRials > 0L || accountNumber.isNotEmpty() || bankName != "بانک من")

        return ParsedBankSms(
            isValid = isValid,
            bankName = bankName,
            accountNumber = accountNumber,
            type = type,
            amountRials = amountRials,
            balanceRials = balanceRials,
            dateString = dateString,
            rawSms = body.trim(),
            sender = sender.trim(),
            suggestedCategory = suggestedCategory
        )
    }
}

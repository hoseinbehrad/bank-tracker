package com.example.sms

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.example.data.model.BankCardEntity
import com.example.data.model.TransactionEntity
import com.example.util.PersianCalendarUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsScanResult(
    val newTransactions: List<TransactionEntity>,
    val duplicateCount: Int
)

object SmsInboxScanner {

    suspend fun scanExistingBankSms(
        context: Context,
        existingTransactions: List<TransactionEntity>,
        configuredCards: List<BankCardEntity> = emptyList(),
        activeMonth: String = "شهریور"
    ): SmsScanResult = withContext(Dispatchers.IO) {
        val newItems = mutableListOf<TransactionEntity>()
        var duplicates = 0
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC LIMIT 500"
            )

            cursor?.use {
                val addressCol = it.getColumnIndex("address")
                val bodyCol = it.getColumnIndex("body")
                val dateCol = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val address = if (addressCol >= 0) it.getString(addressCol) ?: "" else ""
                    val body = if (bodyCol >= 0) it.getString(bodyCol) ?: "" else ""
                    val dateMillis = if (dateCol >= 0) it.getLong(dateCol) else System.currentTimeMillis()

                    if (BankSmsParser.isBankSms(address, body, configuredCards)) {
                        val parsed = BankSmsParser.parse(body, address, configuredCards)

                        // Filter by user's configured bank cards only
                        if (parsed.isValid && matchesAnyConfiguredCard(context, configuredCards, address, body, parsed)) {
                            // Find the matching card to ensure account and bank name are consistent
                            val matchedCard = findMatchingCard(context, configuredCards, address, body, parsed)
                            val finalBankName = matchedCard?.bankName ?: parsed.bankName
                            val acc = if (parsed.accountNumber.isNotBlank() && parsed.accountNumber != "ناشناخته") {
                                parsed.accountNumber
                            } else {
                                matchedCard?.accountNumber ?: "ناشناخته"
                            }

                            // Check if duplicate exists in existing transactions OR newly scanned batch
                            val isDup = (existingTransactions + newItems).any { existing ->
                                isDuplicateTransaction(
                                    existing = existing,
                                    candidateAmount = parsed.amountRials,
                                    candidateType = parsed.type,
                                    candidateDate = parsed.dateString,
                                    candidateTimestamp = dateMillis,
                                    candidateAccount = acc,
                                    candidateRawSms = parsed.rawSms
                                )
                            }

                            if (isDup) {
                                duplicates++
                            } else {
                                val sheet = if (parsed.dateString.isNotBlank()) {
                                    PersianCalendarUtils.extractMonthFromDateString(parsed.dateString)
                                } else {
                                    activeMonth
                                }

                                newItems.add(
                                    TransactionEntity(
                                        accountNumber = acc,
                                        bankName = finalBankName,
                                        type = parsed.type,
                                        amountRials = parsed.amountRials,
                                        balanceRials = parsed.balanceRials,
                                        category = parsed.suggestedCategory.titleFa,
                                        rawSms = parsed.rawSms,
                                        sender = parsed.sender,
                                        dateString = parsed.dateString,
                                        timestamp = dateMillis,
                                        monthSheet = sheet
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Permission or content resolver exception handled gracefully
        }

        return@withContext SmsScanResult(newItems, duplicates)
    }

    private fun findMatchingCard(
        context: Context?,
        cards: List<BankCardEntity>,
        address: String,
        body: String,
        parsed: ParsedBankSms
    ): BankCardEntity? {
        if (cards.isEmpty()) return null
        val normalizedBody = BankSmsParser.normalizePersianText(body)
        val cleanAddr = BankSmsParser.cleanPhoneNumber(address)
        val rawAddr = address.trim().lowercase()

        // 1. Check account number
        for (card in cards) {
            if (card.accountNumber.isNotBlank()) {
                val acc = card.accountNumber.trim()
                if (normalizedBody.contains(acc) || parsed.accountNumber == acc ||
                    (parsed.accountNumber.isNotBlank() && (parsed.accountNumber.endsWith(acc) || acc.endsWith(parsed.accountNumber)))) {
                    return card
                }
            }
        }

        // 2. Check contact phone / address match
        for (card in cards) {
            val cardPhone = BankSmsParser.cleanPhoneNumber(card.contactPhone)
            if (cardPhone.isNotEmpty() && cleanAddr.isNotEmpty()) {
                if (cleanAddr == cardPhone || cleanAddr.endsWith(cardPhone) || cardPhone.endsWith(cleanAddr)) {
                    return card
                }
            }
        }

        // 3. Check contact name in address or body
        for (card in cards) {
            val cardContact = card.senderContact.trim().lowercase()
            if (cardContact.isNotEmpty()) {
                if (rawAddr.contains(cardContact) || normalizedBody.lowercase().contains(cardContact)) {
                    return card
                }
                if (context != null) {
                    val resolved = resolveContactName(context, address)?.lowercase() ?: ""
                    if (resolved.isNotEmpty() && (resolved.contains(cardContact) || cardContact.contains(resolved))) {
                        return card
                    }
                }
            }
        }

        // 4. Check bank name
        for (card in cards) {
            val cardBank = card.bankName.replace("بانک", "").replace("بانك", "").trim()
            val parsedBank = parsed.bankName.replace("بانک", "").replace("بانك", "").trim()
            if (cardBank.isNotEmpty() && (parsedBank.contains(cardBank) || cardBank.contains(parsedBank) || normalizedBody.contains(cardBank))) {
                return card
            }
        }

        return null
    }

    /**
     * Checks if an SMS message matches any of the user's specifically configured cards/contacts.
     * If no cards are configured, permits recognized bank SMS.
     */
    fun matchesAnyConfiguredCard(
        context: Context?,
        cards: List<BankCardEntity>,
        address: String,
        body: String,
        parsed: ParsedBankSms
    ): Boolean {
        if (cards.isEmpty()) return true

        val cleanAddr = BankSmsParser.cleanPhoneNumber(address)
        val rawAddr = address.trim().lowercase()
        val normalizedBody = BankSmsParser.normalizePersianText(body)

        return cards.any { card ->
            val cardPhone = BankSmsParser.cleanPhoneNumber(card.contactPhone)
            val cardContact = card.senderContact.trim().lowercase()
            val cardAcc = card.accountNumber.trim()

            // 1. Account number in body or parsed
            if (cardAcc.isNotEmpty()) {
                if (normalizedBody.contains(cardAcc) ||
                    parsed.accountNumber == cardAcc ||
                    (parsed.accountNumber.isNotBlank() && (parsed.accountNumber.endsWith(cardAcc) || cardAcc.endsWith(parsed.accountNumber)))) {
                    return@any true
                }
            }

            // 2. Contact phone / shortcode / address match
            if (cardPhone.isNotEmpty() && cleanAddr.isNotEmpty()) {
                if (cleanAddr == cardPhone || cleanAddr.endsWith(cardPhone) || cardPhone.endsWith(cleanAddr)) {
                    return@any true
                }
            }

            // 3. Contact name in address, body, or phone lookup
            if (cardContact.isNotEmpty()) {
                if (rawAddr.contains(cardContact) || normalizedBody.lowercase().contains(cardContact)) {
                    return@any true
                }
                if (context != null) {
                    val resolved = resolveContactName(context, address)?.lowercase() ?: ""
                    if (resolved.isNotEmpty() && (resolved.contains(cardContact) || cardContact.contains(resolved))) {
                        return@any true
                    }
                }
            }

            // 4. Bank name match (e.g. Saderat or Mellat)
            val cardBank = card.bankName.lowercase().replace("بانک", "").replace("بانك", "").trim()
            val parsedBank = parsed.bankName.lowercase().replace("بانک", "").replace("بانك", "").trim()
            if (cardBank.isNotEmpty() && (parsedBank.contains(cardBank) || cardBank.contains(parsedBank) || normalizedBody.contains(cardBank))) {
                if (cardAcc.isBlank() || parsed.accountNumber.isBlank() ||
                    parsed.accountNumber == "ناشناخته" || cardAcc == parsed.accountNumber ||
                    normalizedBody.contains(cardAcc)) {
                    return@any true
                }
            }

            false
        }
    }

    private fun resolveContactName(context: Context, phoneNumber: String): String? {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (idx >= 0) return it.getString(idx)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Checks if a candidate SMS transaction is a duplicate.
     * Prevents re-adding identical SMS messages without discarding distinct real transactions.
     */
    fun isDuplicateTransaction(
        existing: TransactionEntity,
        candidateAmount: Long,
        candidateType: String,
        candidateDate: String,
        candidateTimestamp: Long,
        candidateAccount: String,
        candidateRawSms: String
    ): Boolean {
        // Amounts must match
        if (existing.amountRials != candidateAmount) return false

        // Transaction types must match (income vs expense)
        if (!existing.type.equals(candidateType, ignoreCase = true)) return false

        // Account must match if both are present
        if (existing.accountNumber.isNotBlank() && candidateAccount.isNotBlank() &&
            existing.accountNumber != "ناشناخته" && candidateAccount != "ناشناخته" &&
            existing.accountNumber != candidateAccount) {
            return false
        }

        // 1. Raw SMS text is identical
        if (candidateRawSms.isNotBlank() && existing.rawSms.isNotBlank()) {
            if (candidateRawSms.trim() == existing.rawSms.trim()) {
                return true
            }
        }

        // 2. Date string matches exactly (e.g. 05/06/21-22:46 or 0616 - 12:54)
        if (candidateDate.isNotBlank() && existing.dateString.isNotBlank()) {
            if (candidateDate.trim() == existing.dateString.trim()) {
                return true
            }
        }

        // 3. If exact same SMS timestamp from content provider
        if (existing.timestamp > 0 && candidateTimestamp > 0 && existing.timestamp == candidateTimestamp) {
            return true
        }

        return false
    }
}

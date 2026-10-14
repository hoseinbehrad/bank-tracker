package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class CurrencyType(val labelEn: String, val labelFa: String) {
    TOMAN("Toman", "تومان"),
    RIAL("Rial", "ریال")
}

object CurrencyUtils {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /**
     * Converts Persian and Arabic digits to ASCII digits (0-9)
     * and removes commas, spaces, and currency symbols.
     */
    fun normalizeToAsciiDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when {
                ch in '0'..'9' -> sb.append(ch)
                ch in '۰'..'۹' -> sb.append(ch - '۰')
                ch in '٠'..'٩' -> sb.append(ch - '٠')
                ch == '.' -> sb.append('.')
            }
        }
        return sb.toString()
    }

    /**
     * Replaces standard ASCII digits with Persian digits
     */
    fun toPersianDigits(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Formats an amount stored in Rials into either Toman or Rial with comma separators
     */
    fun formatAmount(
        amountRials: Long,
        currency: CurrencyType,
        usePersianDigits: Boolean = true,
        includeUnit: Boolean = true
    ): String {
        val displayAmount = when (currency) {
            CurrencyType.TOMAN -> amountRials / 10
            CurrencyType.RIAL -> amountRials
        }

        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,###", symbols)
        val formattedNumber = formatter.format(displayAmount)

        val finalNumber = if (usePersianDigits) {
            toPersianDigits(formattedNumber)
        } else {
            formattedNumber
        }

        return if (includeUnit) {
            val unit = if (usePersianDigits) currency.labelFa else currency.labelEn
            "$finalNumber $unit"
        } else {
            finalNumber
        }
    }

    /**
     * Parses numeric text from SMS (e.g. "10,850,000" or "۱۰,۸۵۰,۰۰۰") into Long
     */
    fun parseAmount(text: String): Long {
        val clean = normalizeToAsciiDigits(text)
        return clean.toLongOrNull() ?: 0L
    }
}

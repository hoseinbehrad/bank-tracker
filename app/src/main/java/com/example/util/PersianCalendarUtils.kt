package com.example.util

import java.util.Calendar

object PersianCalendarUtils {

    val PERSIAN_MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    /**
     * Given a month name like "مرداد", returns the subsequent month name "شهریور".
     * Wraps around from "اسفند" to "فروردین".
     */
     fun getNextMonthName(currentMonth: String): String {
         val clean = currentMonth.trim()
         // If month contains a known Persian month name
         val matchedIndex = PERSIAN_MONTH_NAMES.indexOfFirst { clean.contains(it) }
         if (matchedIndex >= 0) {
             val nextIndex = (matchedIndex + 1) % PERSIAN_MONTH_NAMES.size
             return PERSIAN_MONTH_NAMES[nextIndex]
         }
         return "ماه جدید"
     }

    /**
     * Extracts or estimates the Persian month name from dateString (e.g. 05/05/05 or 1404/05/05)
     */
    fun extractMonthFromDateString(dateString: String): String {
        val clean = dateString.trim()
        // If string directly contains a month name
        for (m in PERSIAN_MONTH_NAMES) {
            if (clean.contains(m)) return m
        }

        // Format like 05/06/21 or 1405/06/21 or 05/05/05
        val datePart = clean.split("-", " ", "T")[0].trim()
        if (datePart.contains("/")) {
            val parts = datePart.split("/")
            if (parts.size >= 2) {
                val monthNum = parts[1].toIntOrNull()
                if (monthNum != null && monthNum in 1..12) {
                    return PERSIAN_MONTH_NAMES[monthNum - 1]
                }
            }
        }

        // Saderat format like 0601 - 16:57 (MMDD where 06 is month, 01 is day)
        val mmddRegex = Regex("""\b([0-1][0-9])([0-3][0-9])\b""")
        val mmddMatch = mmddRegex.find(clean)
        if (mmddMatch != null) {
            val monthNum = mmddMatch.groupValues[1].toIntOrNull()
            if (monthNum != null && monthNum in 1..12) {
                return PERSIAN_MONTH_NAMES[monthNum - 1]
            }
        }

        return "شهریور" // Default current month for user
    }
}

package com.example.presentation.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Human-readable date and time formatting utility for PakkaKhata.
 * Formats timestamps into natural terms for small-shop ledger entries:
 * - "Today, 6:30 PM"
 * - "Yesterday, 11:20 AM"
 * - "18 Sep, 2:15 PM" (or "18 Sep 2026, 2:15 PM" for other years)
 */
object DateTimeFormatter {

    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Just now"

        val now = Calendar.getInstance()
        val time = Calendar.getInstance().apply { timeInMillis = timestamp }

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date(timestamp))

        val isToday = now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)

        if (isToday) {
            return "Today, $timeStr"
        }

        now.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) {
            return "Yesterday, $timeStr"
        }

        val nowYear = Calendar.getInstance().get(Calendar.YEAR)
        val formatPattern = if (time.get(Calendar.YEAR) == nowYear) {
            "d MMM, h:mm a"
        } else {
            "d MMM yyyy, h:mm a"
        }
        val dateFormat = SimpleDateFormat(formatPattern, Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

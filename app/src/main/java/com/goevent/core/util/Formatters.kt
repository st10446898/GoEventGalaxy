package com.goevent.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object Formatters {

    fun rand(cents: Int): String {
        val sign = if (cents < 0) "-" else ""
        val a = abs(cents)
        val r = a / 100
        val c = a % 100
        return if (c == 0) "${sign}R$r" else "${sign}R$r.${c.toString().padStart(2, '0')}"
    }

    private fun parse(text: String, pattern: String, zone: TimeZone? = null): Date? = try {
        SimpleDateFormat(pattern, Locale.ENGLISH).apply {
            isLenient = false
            if (zone != null) timeZone = zone
        }.parse(text)
    } catch (e: Exception) {
        null
    }

    private fun format(date: Date, pattern: String): String = SimpleDateFormat(pattern, Locale.ENGLISH).format(date)

    /** "2026-11-04" -> "Wed, 4 Nov 2026" */
    fun date(iso: String): String = parse(iso.take(10), "yyyy-MM-dd")?.let { format(it, "EEE, d MMM yyyy") } ?: iso

    /** "2026-11-04" -> "4 Nov" */
    fun shortDate(iso: String): String = parse(iso.take(10), "yyyy-MM-dd")?.let { format(it, "d MMM") } ?: iso

    /** "2026-11-04" -> ("4", "NOV") for date badges */
    fun dayAndMonth(iso: String): Pair<String, String> {
        val d = parse(iso.take(10), "yyyy-MM-dd") ?: return "" to ""
        return format(d, "d") to format(d, "MMM").uppercase(Locale.ENGLISH)
    }

    /** "18:00:00" -> "18:00" */
    fun time(t: String): String = t.take(5)

    fun greeting(hour: Int): String = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }

    fun firstName(full: String): String = full.trim().substringBefore(' ').ifEmpty { "Explorer" }

    fun initials(full: String): String {
        val parts = full.trim().split(' ').filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "G"
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
        }
    }

    fun todayIso(): String = format(Date(), "yyyy-MM-dd")

    fun isUpcoming(eventDate: String): Boolean = eventDate.take(10) >= todayIso()

    /** kind: "Any", "Today", "This week", "This month" -> (from, to) as yyyy-MM-dd (null = no limit). */
    fun dateRange(kind: String): Pair<String?, String?> {
        val today = todayIso()
        val cal = Calendar.getInstance()
        return when (kind) {
            "Today" -> today to today
            "This week" -> {
                cal.add(Calendar.DAY_OF_YEAR, 7)
                today to format(cal.time, "yyyy-MM-dd")
            }
            "This month" -> {
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                today to format(cal.time, "yyyy-MM-dd")
            }
            else -> null to null
        }
    }

    /** "2026-11-03T10:20:30.123+00:00" -> "5 min ago" */
    fun relativeTime(iso: String?, nowMillis: Long = System.currentTimeMillis()): String {
        if (iso.isNullOrBlank()) return ""
        val then = parse(iso.take(19).replace('T', ' '), "yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("UTC"))
            ?: return ""
        val minutes = (nowMillis - then.time) / 60_000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 60 * 24 -> "${minutes / 60} h ago"
            minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)} d ago"
            else -> shortDate(iso)
        }
    }
}
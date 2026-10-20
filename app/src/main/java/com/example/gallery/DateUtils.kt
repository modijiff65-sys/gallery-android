package com.example.gallery

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun dayKey(ms: Long): String = dayFmt.format(Date(ms))

    fun headerLabel(ctx: Context, ms: Long): String {
        val now = Calendar.getInstance()
        val d = Calendar.getInstance().apply { timeInMillis = ms }
        val sameDay = now.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return ctx.getString(R.string.today)

        val y = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = y.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
            y.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) return ctx.getString(R.string.yesterday)

        return if (now.get(Calendar.YEAR) == d.get(Calendar.YEAR)) {
            SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(ms))
        } else {
            SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(ms))
        }
    }

    fun fullStamp(ms: Long): String =
        SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(ms))

    fun duration(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun size(bytes: Long): String {
        if (bytes <= 0) return "—"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) String.format(Locale.getDefault(), "%.2f MB", mb)
        else String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}

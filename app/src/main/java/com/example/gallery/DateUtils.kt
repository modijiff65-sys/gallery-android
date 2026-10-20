package com.example.gallery

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    const val MODE_ALL = 0
    const val MODE_YEARS = 1
    const val MODE_MONTHS = 2
    const val MODE_DAYS = 3

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthFmt = SimpleDateFormat("yyyy-MM", Locale.US)
    private val yearFmt = SimpleDateFormat("yyyy", Locale.US)

    fun groupKey(ms: Long, mode: Int): String = when (mode) {
        MODE_YEARS -> yearFmt.format(Date(ms))
        MODE_MONTHS -> monthFmt.format(Date(ms))
        else -> dayFmt.format(Date(ms))
    }

    fun groupLabel(ctx: Context, ms: Long, mode: Int): String {
        val now = Calendar.getInstance()
        val d = Calendar.getInstance().apply { timeInMillis = ms }
        return when (mode) {
            MODE_YEARS -> SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(ms))
            MODE_MONTHS -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(ms))
            else -> {
                val sameDay = now.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
                if (sameDay) {
                    ctx.getString(R.string.today)
                } else {
                    val y = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    val isYesterday = y.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
                        y.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
                    if (isYesterday) ctx.getString(R.string.yesterday)
                    else if (now.get(Calendar.YEAR) == d.get(Calendar.YEAR))
                        SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(ms))
                    else
                        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(ms))
                }
            }
        }
    }

    fun fullStamp(ms: Long): String =
        SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(ms))

    fun dayAndTime(ms: Long): Pair<String, String> {
        val day = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(ms))
        val time = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(ms))
        return day to time
    }

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

    fun megapixels(w: Int, h: Int): String {
        if (w <= 0 || h <= 0) return "—"
        val mp = (w.toLong() * h.toLong()) / 1_000_000.0
        return String.format(Locale.getDefault(), "%.1f MP", mp)
    }
}

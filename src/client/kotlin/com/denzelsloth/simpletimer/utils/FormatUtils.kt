package com.denzelsloth.simpletimer.utils

object FormatUtils {
    fun String.stripFormatting(): String =
        replace(Regex("\u00a7[0-9a-fk-or]"), "").trim()

    fun formatInterval(millis: Long): String {
        val totalSeconds = millis / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    fun formatSize(size: Float): String {
        if (size == size.toInt().toFloat()) {
            return size.toInt().toString()
        }
        return String.format("%.2f", size).trimEnd('0').trimEnd('.')
    }
}

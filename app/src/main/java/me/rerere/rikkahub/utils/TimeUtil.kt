package me.rerere.rikkahub.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun Instant.toLocalDate(): String {
    val zoneId = ZoneId.systemDefault()
    val localDateTime = this.atZone(zoneId).toLocalDateTime()

    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(localDateTime)
}

fun Instant.toLocalDateTime(): String {
    val zoneId = ZoneId.systemDefault()
    val localDateTime = this.atZone(zoneId).toLocalDateTime()

    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(localDateTime)
}
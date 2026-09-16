package github.detrig.feature.gamesession.presentation.formatter

import java.text.NumberFormat
import java.util.Locale

internal fun Long.toRubText(): String {
    return "${rubFormatter.format(this)} ₽"
}

internal fun Long.toAllowanceCountdownText(nowMillis: Long): String {
    val remainingMillis = (this - nowMillis).coerceAtLeast(0)
    if (remainingMillis == 0L) return "сейчас"

    val hours = (remainingMillis + MILLIS_PER_HOUR - 1) / MILLIS_PER_HOUR
    if (hours < HOURS_PER_DAY) return "через $hours ч"

    val days = (hours + HOURS_PER_DAY - 1) / HOURS_PER_DAY
    return "через $days ${days.toInt().dayWord()}"
}

private fun Int.dayWord(): String {
    val lastTwoDigits = this % 100
    val lastDigit = this % 10
    return when {
        lastTwoDigits in 11..14 -> "дней"
        lastDigit == 1 -> "день"
        lastDigit in 2..4 -> "дня"
        else -> "дней"
    }
}

private const val MILLIS_PER_HOUR = 60L * 60 * 1_000
private const val HOURS_PER_DAY = 24L

private val rubFormatter: NumberFormat = NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU"))

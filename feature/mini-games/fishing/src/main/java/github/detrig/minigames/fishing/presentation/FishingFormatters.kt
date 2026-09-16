package github.detrig.minigames.fishing.presentation

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatFishingMass(grams: Int): String =
    if (grams < 1000) "$grams г" else String.format(Locale.forLanguageTag("ru"), "%.2f кг", grams / 1000.0)
internal fun formatFishingDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru")).format(Date(millis))

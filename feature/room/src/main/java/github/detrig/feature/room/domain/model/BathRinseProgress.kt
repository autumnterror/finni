package github.detrig.feature.room.domain.model

/** Dirt already rinsed away cannot return when another soap cycle adds foam. */
internal fun remainingDirtAfterRinse(current: Float, remainingFoam: Float, peakFoam: Float): Float =
    if (peakFoam <= 0f) current else
        minOf(current, remainingFoam / peakFoam).coerceIn(0f, 1f)

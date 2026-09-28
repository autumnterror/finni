package github.detrig.feature.room.domain.model

/** Dirt already rinsed away cannot return when another soap cycle adds foam. */
internal fun remainingDirtAfterRinse(current: Float, remainingFoam: Int, peakFoam: Int): Float =
    if (peakFoam <= 0) current else
        minOf(current, remainingFoam.toFloat() / peakFoam).coerceIn(0f, 1f)

package github.detrig.core.time

data class ElapsedIntervals(
    val count: Long,
    val checkpointMillis: Long,
)

/** Keeps the remainder, so frequent checks do not postpone the next interval. */
fun elapsedIntervals(checkpointMillis: Long, nowMillis: Long, intervalMillis: Long): ElapsedIntervals {
    require(intervalMillis > 0)
    if (checkpointMillis <= 0L) return ElapsedIntervals(0, nowMillis.coerceAtLeast(0))
    if (nowMillis <= checkpointMillis) return ElapsedIntervals(0, checkpointMillis)
    val count = (nowMillis - checkpointMillis) / intervalMillis
    return ElapsedIntervals(count, checkpointMillis + count * intervalMillis)
}

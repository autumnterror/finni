package github.detrig.minigames.flight.domain

/** Фиксированный шаг с ограниченным догонянием. Просадка FPS не открывает паузу. */
class FlightFrameClock(private val tickRate: Int, private val maxFrameMillis: Long) {
    data class Batch(val ticks: Int = 0, val fraction: Float = 0f)
    private var previous: Long? = null
    private var scaledRemainder = 0L

    fun reset() { previous = null; scaledRemainder = 0 }
    fun frame(nanos: Long): Batch {
        val before = previous
        previous = nanos
        if (before == null) return Batch()
        if (nanos < before) { scaledRemainder = 0; return Batch() }
        val delta = (nanos - before).coerceAtMost(maxFrameMillis * 1_000_000)
        scaledRemainder += delta * tickRate
        val ticks = (scaledRemainder / 1_000_000_000).toInt()
        scaledRemainder %= 1_000_000_000
        return Batch(ticks, scaledRemainder / 1_000_000_000f)
    }
}
package github.detrig.core.time

/**
 * Reconciles one persisted domain against a shared wall-clock instant.
 * Each task owns its checkpoint and must persist effects with that checkpoint atomically.
 * Repeated calls with the same instant must not apply an effect twice.
 */
fun interface TimeDrivenTask {
    suspend fun reconcile(nowMillis: Long)
}

class TimedEventProcessor(
    private val clock: WallClock,
    private val tasks: List<TimeDrivenTask>,
) {
    suspend fun reconcile() {
        val nowMillis = clock.nowMillis()
        tasks.forEach { it.reconcile(nowMillis) }
    }
}

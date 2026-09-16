package github.detrig.minigames.flight

import github.detrig.minigames.flight.domain.*
import java.io.File
import org.junit.Test

/** Диагностика аллокаций физики; время JVM не является измерением FPS устройства. */
class FlightPerformanceTest {
    @Test fun reportSimulationCost() {
        val config = FlightConfig.decode(File("src/main/assets/flight_balance.json").readText())
        val engine = FlightEngine(config)
        // Один и тот же безопасный участок и одинаковые 2 шага для сравнения реализаций.
        val start = engine.create("benchmark", "p", "pet", 7, 0).copy(started = true)
        var checksum = 0.0
        repeat(50_000) { checksum += engine.advance(start, 2).y }
        val bean = Class.forName("java.lang.management.ManagementFactory")
            .getMethod("getThreadMXBean").invoke(null)
        val bytes = Class.forName("com.sun.management.ThreadMXBean")
            .getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)
        val threadId = Thread.currentThread().id
        val allocations = bytes.invoke(bean, threadId) as Long
        val nanos = System.nanoTime()
        repeat(200_000) { checksum += engine.advance(start, 2).y }
        val elapsed = System.nanoTime() - nanos
        val allocated = (bytes.invoke(bean, threadId) as Long) - allocations
        println("flight physics: ${allocated / 200_000} bytes/frame, ${elapsed / 200_000} ns/frame, checksum=$checksum")
    }
}

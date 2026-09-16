package github.detrig.minigames.flight

import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.minigames.flight.domain.*
import github.detrig.minigames.flight.presentation.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Настоящие Choreographer-кадры, без виртуальных часов Compose-теста. */
@RunWith(AndroidJUnit4::class)
class FlightRenderingTest {
    @Test fun reportFullScreenFrameCost() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = FlightConfig.decode(context.assets.open("flight_balance.json").bufferedReader().use { it.readText() })
        val engine = FlightEngine(config)
        val artwork = loadFlightArtwork(context.resources)
        val done = CountDownLatch(1)
        val measuring = AtomicBoolean(false)
        val drawTimes = mutableListOf<Long>()
        val totalTimes = mutableListOf<Long>()
        val thread = HandlerThread("FlightFrameMetrics").apply { start() }
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            if (measuring.get()) synchronized(drawTimes) {
                drawTimes.add(metrics.getMetric(FrameMetrics.DRAW_DURATION))
                totalTimes.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION))
            }
        }
        var finalScore = 0
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                        .hide(WindowInsetsCompat.Type.systemBars())
                    activity.window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
                    activity.setContent {
                        val frame = remember { mutableStateOf(FlightRenderFrame()) }
                        FinPetTheme {
                            FlightScene(frame, config, artwork, false, false, "Взмах", {}, Modifier.fillMaxSize())
                        }
                        LaunchedEffect(Unit) {
                            var session = engine.launch(engine.create("render", "test", "pet", 453, 0))
                            val clock = FlightFrameClock(config.physics.tickRate, config.physics.maxFrameMillis)
                            repeat(360) { index ->
                                withFrameNanos { nanos ->
                                    val batch = clock.frame(nanos)
                                    repeat(batch.ticks) {
                                        val gate = session.gates.firstOrNull { it.x + config.gates.width >= config.world.petX - config.world.radius }
                                        if (session.velocity >= 0 && session.y >= (gate?.center ?: config.world.startY) + 18)
                                            session = engine.flap(session)
                                        session = engine.step(session)
                                    }
                                    frame.value = FlightRenderFrame(session, batch.fraction)
                                    if (index == 60) {
                                        check(activity.hasWindowFocus()) { "A system dialog covers the renderer" }
                                        measuring.set(true)
                                    }
                                }
                            }
                            finalScore = session.score
                            done.countDown()
                        }
                    }
                }
                assertTrue("Renderer did not finish 360 frames", done.await(45, TimeUnit.SECONDS))
                scenario.onActivity { it.window.removeOnFrameMetricsAvailableListener(listener) }
            }
            synchronized(drawTimes) {
                assertTrue("No Android frame metrics", drawTimes.size >= 100)
                assertTrue("Simulation did not progress", finalScore > 0)
                fun percentile(values: List<Long>, p: Double) = values.sorted()[(values.size * p).toInt().coerceAtMost(values.lastIndex)] / 1_000_000.0
                println("FLIGHT_RENDER frames=${drawTimes.size} draw_p50_ms=${percentile(drawTimes, .5)} draw_p95_ms=${percentile(drawTimes, .95)} total_p50_ms=${percentile(totalTimes, .5)} total_p95_ms=${percentile(totalTimes, .95)} score=$finalScore")
                android.util.Log.i("FlightRenderMetrics", "frames=${drawTimes.size} draw_p50_ms=${percentile(drawTimes, .5)} draw_p95_ms=${percentile(drawTimes, .95)} total_p50_ms=${percentile(totalTimes, .5)} total_p95_ms=${percentile(totalTimes, .95)}")
            }
        } finally { thread.quitSafely() }
    }
}

package github.detrig.minigames.fishing

import android.media.SoundPool
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FishingAudioTest {
    @Test fun everyActionCueCanBeDecodedFromThePackagedApk() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cues = listOf("charge", "cast", "splash", "bite", "reel", "relax", "warning", "catch", "release", "escape", "junk", "hazard", "pop")
        val pending = CountDownLatch(cues.size)
        val errors = CopyOnWriteArrayList<Int>()
        val pool = SoundPool.Builder().setMaxStreams(3).build()
        try {
            pool.setOnLoadCompleteListener { _, _, status ->
                if (status != 0) errors.add(status)
                pending.countDown()
            }
            cues.forEach { name ->
                context.assets.openFd("fishing/audio/$name.wav").use { assertTrue(pool.load(it, 1) > 0) }
            }
            assertTrue("SoundPool did not finish decoding all action cues", pending.await(10, TimeUnit.SECONDS))
            assertTrue("Audio decoder errors: $errors", errors.isEmpty())
        } finally { pool.release() }
    }
}

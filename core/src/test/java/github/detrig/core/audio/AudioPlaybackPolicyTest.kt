package github.detrig.core.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPlaybackPolicyTest {
    private val quiet = AudioCue("reel", "fishing", "reel.wav", priority = 0,
        cooldownMillis = 120, blockMillis = 150)
    private val important = AudioCue("catch", "fishing", "catch.wav", priority = 3,
        blockMillis = 500)

    @Test fun importantCueInterruptsQuietCueAndQuietCueWaits() {
        val policy = AudioPlaybackPolicy()
        policy.recordPlayed(quiet, 1000)
        assertTrue(policy.canPlay(important, 1010))
        policy.recordPlayed(important, 1010)
        assertFalse(policy.canPlay(quiet, 1200))
        assertTrue(policy.canPlay(quiet, 1510))
    }

    @Test fun repeatedCueIsThrottledUntilCooldownExpires() {
        val policy = AudioPlaybackPolicy()
        policy.recordPlayed(quiet, 1000)
        assertFalse(policy.canPlay(quiet, 1050))
        assertTrue(policy.canPlay(quiet, 1120))
    }

    @Test fun stoppingOwnerClearsPriorityBlock() {
        val policy = AudioPlaybackPolicy()
        policy.recordPlayed(important, 1000)
        policy.stop("fishing")
        assertTrue(policy.canPlay(quiet, 1010))
    }
}

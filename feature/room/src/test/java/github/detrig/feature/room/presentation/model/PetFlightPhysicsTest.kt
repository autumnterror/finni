package github.detrig.feature.room.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetFlightPhysicsTest {
    @Test
    fun `flight rebounds from a wall inside its room`() {
        val result = PetFlightPhysics.step(
            frame = PetFlightFrame(x = 1.02f, lift = 0.5f, velocityX = -2f),
            elapsedSeconds = 0.03f,
            left = 1f,
            right = 2f,
            maxLift = 2f,
        )

        assertTrue(result.frame.x >= 1f)
        assertTrue(result.frame.velocityX > 0f)
        assertTrue(!result.landed)
    }

    @Test
    fun `floor allows one soft bounce and then settles`() {
        var frame = PetFlightFrame(x = 1.5f, lift = 0.3f, velocityUp = -2f)
        var bounced = false
        var landed = false
        repeat(120) {
            if (landed) return@repeat
            val step = PetFlightPhysics.step(frame, 0.03f, 1f, 2f, 2f)
            frame = step.frame
            bounced = bounced || frame.floorBounces == 1
            landed = step.landed
        }

        assertTrue(bounced)
        assertTrue(landed)
        assertEquals(0f, frame.lift, 0f)
    }

    @Test
    fun `ceiling and floor keep pet in playable area`() {
        var frame = PetFlightFrame(x = 1.5f, lift = 0.9f, velocityUp = 3f)
        repeat(150) {
            val result = PetFlightPhysics.step(frame, 0.03f, 1f, 2f, 1f)
            frame = result.frame
            assertTrue(frame.lift in 0f..1f)
            assertTrue(frame.x in 1f..2f)
        }
    }
}

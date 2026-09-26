package github.detrig.feature.room.presentation.model

import kotlin.math.abs

/** Distances use one viewport width as the unit; positive lift points up from the floor. */
internal data class PetFlightFrame(
    val x: Float,
    val lift: Float,
    val velocityX: Float = 0f,
    val velocityUp: Float = 0f,
    val floorBounces: Int = 0,
)

internal data class PetFlightStep(val frame: PetFlightFrame, val landed: Boolean)

internal object PetFlightPhysics {
    private const val GRAVITY = 4.8f
    private const val WALL_REBOUND = 0.42f
    private const val FLOOR_REBOUND = 0.22f
    private const val MIN_FLOOR_REBOUND_SPEED = 0.9f
    private const val MAX_FLOOR_BOUNCES = 1
    private const val MAX_STEP_SECONDS = 0.033f

    fun step(
        frame: PetFlightFrame,
        elapsedSeconds: Float,
        left: Float,
        right: Float,
        maxLift: Float,
    ): PetFlightStep {
        require(left <= right)
        val dt = elapsedSeconds.coerceIn(0f, MAX_STEP_SECONDS)
        if (dt == 0f) return PetFlightStep(frame, landed = false)

        var x = frame.x + frame.velocityX * dt
        var vx = frame.velocityX
        if (x < left) {
            x = left + (left - x).coerceAtMost(right - left)
            vx = abs(vx) * WALL_REBOUND
        } else if (x > right) {
            x = right - (x - right).coerceAtMost(right - left)
            vx = -abs(vx) * WALL_REBOUND
        }

        var lift = frame.lift + frame.velocityUp * dt - GRAVITY * dt * dt / 2f
        var vy = frame.velocityUp - GRAVITY * dt
        if (lift >= maxLift) {
            lift = maxLift
            vy = vy.coerceAtMost(0f)
        }

        if (lift <= 0f) {
            if (frame.floorBounces < MAX_FLOOR_BOUNCES && vy < -MIN_FLOOR_REBOUND_SPEED) {
                return PetFlightStep(
                    PetFlightFrame(x, 0f, vx * 0.55f, -vy * FLOOR_REBOUND, frame.floorBounces + 1),
                    landed = false,
                )
            }
            return PetFlightStep(PetFlightFrame(x, 0f), landed = true)
        }
        return PetFlightStep(PetFlightFrame(x, lift, vx, vy, frame.floorBounces), landed = false)
    }
}

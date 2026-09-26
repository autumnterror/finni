package github.detrig.feature.room.presentation.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import github.detrig.feature.room.domain.model.HousePosition
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign

/** Частые координаты принадлежат сцене, а не LiveData экономики. */
@Stable
internal class HouseMotionState(position: HousePosition) {
    private val initial = HouseLayout.restored(position)
    var cameraLeftX by mutableFloatStateOf(initial.cameraLeftX)
    var petX by mutableFloatStateOf(initial.petX)
        private set
    var facingRight by mutableStateOf(initial.facingRight)
        private set
    var isWalking by mutableStateOf(false)
        private set
    var walkPhase by mutableFloatStateOf(0f)
        private set
    var hintSeen by mutableStateOf(initial.hintSeen)
    val targetX get() = HouseLayout.clampPet(cameraLeftX + HouseLayout.VIEWPORT_WIDTH / 2)
    val needsStep get() = abs(targetX - petX) > HouseLayout.PET_STOP_DISTANCE

    fun step(elapsedSeconds: Float) {
        val difference = targetX - petX
        if (abs(difference) <= HouseLayout.PET_STOP_DISTANCE) {
            isWalking = false
            return
        }
        val dt = elapsedSeconds.coerceIn(0f, 0.05f)
        if (dt == 0f) return
        facingRight = difference > 0
        val distance = abs(difference)
        // Точное интегрирование замедления: частота кадров не меняет скорость.
        val slow = HouseLayout.PET_SLOWDOWN_DISTANCE
        val speed = HouseLayout.PET_SPEED
        val fullSpeedTime = ((distance - slow) / speed).coerceAtLeast(0f)
        val remaining = if (dt <= fullSpeedTime) distance - speed * dt
        else minOf(distance, slow) * exp(-speed * (dt - fullSpeedTime) / slow)
        val movement = distance - remaining
        petX = HouseLayout.clampPet(petX + sign(difference) * movement)
        walkPhase = (walkPhase + movement / HouseLayout.WALK_CYCLE_DISTANCE) % 1f
        isWalking = needsStep
    }

    fun pause() { isWalking = false }

    fun placeAt(x: Float) {
        petX = HouseLayout.clampPet(x)
        isWalking = false
    }

    fun position() = HousePosition(
        HouseLayout.VERSION, HouseLayout.clampCamera(cameraLeftX), petX, facingRight, hintSeen,
    )

    companion object {
        val Saver = listSaver<HouseMotionState, Any>(
            save = { with(it.position()) { listOf(layoutVersion, cameraLeftX, petX, facingRight, hintSeen) } },
            restore = { HouseMotionState(HousePosition(it[0] as Int, it[1] as Float, it[2] as Float, it[3] as Boolean, it[4] as Boolean)) },
        )
    }
}

package github.detrig.feature.room.api

import androidx.compose.ui.geometry.Offset

/** Transient room motion; the pet feature remains responsible for drawing its own poses. */
enum class RoomPetPose { IDLE, HELD, AIRBORNE, LANDED, GETTING_UP }

data class RoomPetInteraction(
    val pose: RoomPetPose = RoomPetPose.IDLE,
    val showShadow: Boolean = true,
    val isBathing: Boolean = false,
    val dirtStageOverride: Int? = null,
    val dirtOpacity: Float = 1f,
    val canGrab: Boolean = false,
    val onGrab: () -> Unit = {},
    val onDrag: (Offset) -> Unit = {},
    val onRelease: (Offset) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onTouchStart: () -> Unit = {},
    val onTap: () -> Unit = {},
)

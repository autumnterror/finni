package github.detrig.feature.pet.api

import androidx.compose.ui.geometry.Offset

/** Visual poses share the same layered appearance and future outfit coordinates. */
enum class PetPose { IDLE, HELD, AIRBORNE, LANDED, GETTING_UP }

data class PetGestureCallbacks(
    val onGrab: () -> Unit,
    val onDrag: (Offset) -> Unit,
    val onRelease: (Offset) -> Unit,
    val onCancel: () -> Unit,
    val onTouchStart: () -> Unit = {},
)

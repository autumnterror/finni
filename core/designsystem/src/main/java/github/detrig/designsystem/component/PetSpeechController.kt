package github.detrig.designsystem.component

import androidx.compose.runtime.compositionLocalOf

/** App-owned speech actions used only by pet dialogue surfaces. */
class PetSpeechController(
    val speak: (String) -> Unit,
    val stop: () -> Unit,
)

val LocalPetSpeechController = compositionLocalOf<PetSpeechController?> { null }

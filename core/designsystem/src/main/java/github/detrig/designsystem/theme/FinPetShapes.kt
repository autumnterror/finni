package github.detrig.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.foundation.shape.CornerBasedShape

@Immutable
data class FinPetShapes(
    val compact: CornerBasedShape,
    val card: CornerBasedShape,
    val button: CornerBasedShape,
    val badge: CornerBasedShape,
    val dialog: CornerBasedShape,
    val sheet: CornerBasedShape,
    val storefrontControl: CornerBasedShape = button,
)

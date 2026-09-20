package github.detrig.feature.pet.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import github.detrig.feature.pet.domain.model.PetProfile
import kotlin.math.roundToInt

/** Цветной крупный план мордочки для диалогов и будущих экранов обучения. */
@Composable
fun PetPortrait(profile: PetProfile, modifier: Modifier = Modifier) {
    if (profile.species == github.detrig.feature.pet.domain.model.PetSpecies.Hamster) {
        val assets = rememberHamsterAssets()
        if (assets != null) {
            HamsterPreview(
                assets = assets,
                appearance = profile.hamsterAppearance,
                modifier = modifier,
            )
        }
        return
    }
    val artwork = profile.species.artwork()
    val base = ImageBitmap.imageResource(artwork.baseRes)
    val mask = ImageBitmap.imageResource(artwork.colorMaskRes)
    val tint = profile.color.colorFilter()
    val crop = artwork.faceCrop
    val sourceOffset = IntOffset(
        (base.width * crop.left).roundToInt(),
        (base.height * crop.top).roundToInt(),
    )
    val sourceSize = IntSize(
        (base.width * crop.side).roundToInt(),
        (base.height * crop.side).roundToInt(),
    )

    Canvas(modifier) {
        val destination = IntSize(size.width.roundToInt(), size.height.roundToInt())
        drawImage(
            image = base,
            srcOffset = sourceOffset,
            srcSize = sourceSize,
            dstSize = destination,
            filterQuality = FilterQuality.None,
        )
        drawImage(
            image = mask,
            srcOffset = sourceOffset,
            srcSize = sourceSize,
            dstSize = destination,
            colorFilter = tint,
            filterQuality = FilterQuality.None,
        )
    }
}

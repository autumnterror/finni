package github.detrig.feature.pet.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter as AndroidColorMatrixColorFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.Rect
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.pet.R
import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.model.PetSpecies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Единый 2D-рендер питомца для комнаты и других игровых сцен. */
@Composable
fun PetScene(
    profile: PetProfile,
    modifier: Modifier = Modifier,
    animateIdle: Boolean = true,
) {
    val shadowColor = AppTheme.colors.sceneShadow
    val speciesName = profile.species.title()
    val description = stringResource(R.string.pet_content_description, profile.name, speciesName)
    val hamsterAssets = rememberHamsterAssets()
    val hamsterBlink = if (profile.species == PetSpecies.Hamster) rememberHamsterBlink() else false
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawOval(
                color = shadowColor,
                topLeft = Offset(size.width * 0.24f, size.height * 0.88f),
                size = Size(size.width * 0.52f, size.height * 0.08f),
            )
        }
        Box(Modifier.fillMaxSize().petCalmIdleAnimation(animateIdle)) {
            if (profile.species == PetSpecies.Hamster && hamsterAssets != null) {
                HamsterPreview(
                    assets = hamsterAssets,
                    appearance = profile.hamsterAppearance,
                    modifier = Modifier.fillMaxSize(),
                    blink = hamsterBlink,
                )
            } else if (profile.species != PetSpecies.Hamster) {
                Image(
                    bitmap = ImageBitmap.imageResource(profile.species.artwork().baseRes),
                    contentDescription = description,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                )
                Image(
                    bitmap = ImageBitmap.imageResource(profile.species.artwork().colorMaskRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = profile.color.colorFilter(),
                    filterQuality = FilterQuality.None,
                )
            }
        }
    }
}

@Composable
internal fun rememberPetAppearanceBitmap(
    profile: PetProfile,
    maxSidePx: Int,
): ImageBitmap? {
    require(maxSidePx > 0)
    if (profile.species == PetSpecies.Hamster) {
        val assets = rememberHamsterAssets()
        val blink = rememberHamsterBlink()
        val bitmap by produceState<ImageBitmap?>(
            initialValue = null,
            assets,
            profile.hamsterAppearance,
            blink,
            maxSidePx,
        ) {
            value = assets?.let {
                withContext(Dispatchers.Default) {
                    it.renderBitmap(profile.hamsterAppearance, maxSidePx, blink).asImageBitmap()
                }
            }
        }
        return bitmap
    }
    val resources = LocalResources.current
    val tint = profile.color.tint()
    val tintArgb = tint.toArgb()
    val bitmap by produceState<ImageBitmap?>(
        initialValue = null,
        resources,
        profile.species,
        tintArgb,
        maxSidePx,
    ) {
        value = withContext(Dispatchers.Default) {
            val artwork = profile.species.artwork()
            val base = decodeSampledBitmap(resources, artwork.baseRes, maxSidePx)
            val mask = decodeSampledBitmap(resources, artwork.colorMaskRes, maxSidePx)
            try {
                createBitmap(maxSidePx, maxSidePx).also { output ->
                    val target = Rect(0, 0, maxSidePx, maxSidePx)
                    val canvas = AndroidCanvas(output)
                    val paint = AndroidPaint().apply { isFilterBitmap = false }
                    canvas.drawBitmap(base, null, target, paint)
                    paint.colorFilter = AndroidColorMatrixColorFilter(AndroidColorMatrix(tintMatrix(tint)))
                    canvas.drawBitmap(mask, null, target, paint)
                }.asImageBitmap()
            } finally {
                base.recycle()
                mask.recycle()
            }
        }
    }
    return bitmap
}

private fun decodeSampledBitmap(
    resources: android.content.res.Resources,
    drawableRes: Int,
    targetSidePx: Int,
): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(resources, drawableRes, bounds)
    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= targetSidePx) {
        sampleSize *= 2
    }
    return requireNotNull(
        BitmapFactory.decodeResource(
            resources,
            drawableRes,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inScaled = false
            },
        ),
    )
}

@Composable
internal fun PetSpecies.title(): String = stringResource(
    when (this) {
        PetSpecies.Hamster -> R.string.pet_species_hamster
        PetSpecies.Cat -> R.string.pet_species_cat
        PetSpecies.Dog -> R.string.pet_species_dog
        PetSpecies.Rat -> R.string.pet_species_rat
        PetSpecies.Rooster -> R.string.pet_species_rooster
    },
)

@Composable
internal fun PetColor.title(): String = stringResource(
    when (this) {
        PetColor.Sunny -> R.string.pet_color_sunny
        PetColor.Mint -> R.string.pet_color_mint
        PetColor.Coral -> R.string.pet_color_coral
        PetColor.Sky -> R.string.pet_color_sky
    },
)

@Composable
internal fun PetColor.tint(): Color = when (this) {
    PetColor.Sunny -> AppTheme.colors.petColorSunny
    PetColor.Mint -> AppTheme.colors.petColorMint
    PetColor.Coral -> AppTheme.colors.petColorCoral
    PetColor.Sky -> AppTheme.colors.petColorSky
}

@Composable
internal fun PetColor.colorFilter(): ColorFilter {
    val color = tint()
    return ColorFilter.colorMatrix(
        ColorMatrix(tintMatrix(color)),
    )
}

private fun tintMatrix(color: Color) = floatArrayOf(
    color.red, 0f, 0f, 0f, 0f,
    0f, color.green, 0f, 0f, 0f,
    0f, 0f, color.blue, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

@Composable
private fun Modifier.petCalmIdleAnimation(enabled: Boolean): Modifier {
    if (!enabled) return this
    val transition = rememberInfiniteTransition(label = "pet_calm_idle")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AppTheme.motion.durationSlowMillis * 3,
                easing = AppTheme.motion.standardEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pet_calm_idle_progress",
    )
    val translationY = with(LocalDensity.current) { -AppTheme.spacing.xs.toPx() }
    return graphicsLayer {
        transformOrigin = TransformOrigin(0.5f, 0.92f)
        scaleX = 1f + 0.006f * progress
        scaleY = 1f + 0.018f * progress
        this.translationY = translationY * progress
    }
}

package github.detrig.feature.pet.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter as AndroidColorMatrixColorFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.Rect
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.createBitmap
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.pet.R
import github.detrig.feature.pet.api.PetGestureCallbacks
import github.detrig.feature.pet.api.PetPose
import github.detrig.feature.pet.domain.model.HamsterAppearance
import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.model.PetSpecies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.PI
import kotlin.math.sin

/** Единый 2D-рендер питомца для комнаты и других игровых сцен. */
@Composable
fun PetScene(
    profile: PetProfile,
    modifier: Modifier = Modifier,
    animateIdle: Boolean = true,
    mouthOpen: Boolean = false,
    lookAt: Offset? = null,
    onClick: (() -> Unit)? = null,
    pose: PetPose = PetPose.IDLE,
    gestureCallbacks: PetGestureCallbacks? = null,
    showShadow: Boolean = true,
) {
    val shadowColor = AppTheme.colors.sceneShadow
    val speciesName = profile.species.title()
    val description = stringResource(R.string.pet_content_description, profile.name, speciesName)
    val hamsterAssets = if (profile.species == PetSpecies.Hamster) rememberHamsterAssets() else null
    val hamsterBlink = if (profile.species == PetSpecies.Hamster) rememberHamsterBlink() else false
    val reaction = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var reacting by remember { mutableStateOf(false) }
    var reactionJob by remember { mutableStateOf<Job?>(null) }
    var reactionSerial by remember { mutableIntStateOf(0) }
    val poseScaleX by animateFloatAsState(
        targetValue = when (pose) {
            PetPose.HELD -> 0.94f
            PetPose.AIRBORNE -> 0.97f
            PetPose.LANDED -> 1.17f
            PetPose.GETTING_UP -> 0.91f
            PetPose.IDLE -> 1f
        },
        animationSpec = tween(if (pose == PetPose.LANDED) 65 else 180),
        label = "pet_pose_scale_x",
    )
    val poseScaleY by animateFloatAsState(
        targetValue = when (pose) {
            PetPose.HELD -> 1.04f
            PetPose.AIRBORNE -> 1.05f
            PetPose.LANDED -> 0.78f
            PetPose.GETTING_UP -> 1.14f
            PetPose.IDLE -> 1f
        },
        animationSpec = tween(if (pose == PetPose.LANDED) 65 else 180),
        label = "pet_pose_scale_y",
    )
    val tapScaleX = 1f + reaction.value * 0.05f
    val tapScaleY = 1f - reaction.value * 0.06f
    val flightPhase = if (pose == PetPose.HELD || pose == PetPose.AIRBORNE) {
        val flightTransition = rememberInfiniteTransition(label = "pet_flight")
        val phase by flightTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (pose == PetPose.HELD) 800 else 620,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "pet_flight_phase",
        )
        phase
    } else null
    val limbAmplitude = if (pose == PetPose.HELD) 0.8f else 1f
    val flightWobble = flightPhase?.let { sin(it * 2f * PI).toFloat() * limbAmplitude } ?: 0f
    val viewConfiguration = LocalViewConfiguration.current
    val reactThenClick = {
        if (pose == PetPose.IDLE) {
            reactionSerial++
            val serial = reactionSerial
            reactionJob?.cancel()
            reacting = true
            reactionJob = scope.launch {
                try {
                    reaction.snapTo(0f)
                    reaction.animateTo(1f, tween(85))
                    reaction.animateTo(
                        0f,
                        spring(dampingRatio = Spring.DampingRatioNoBouncy),
                    )
                    onClick?.invoke()
                } finally {
                    if (serial == reactionSerial) reacting = false
                }
            }
        }
    }
    val clickModifier = when {
        profile.species == PetSpecies.Hamster && hamsterAssets != null -> modifier.hamsterClickable(
            assets = hamsterAssets,
            appearance = profile.hamsterAppearance,
            blink = hamsterBlink,
            contentDescription = description,
            onClick = reactThenClick.takeIf { pose == PetPose.IDLE },
            gestureCallbacks = gestureCallbacks,
            touchSlop = viewConfiguration.touchSlop,
            longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis,
            scaleX = poseScaleX * tapScaleX,
            scaleY = poseScaleY * tapScaleY,
        )
        onClick != null -> modifier.clickable(
            onClickLabel = stringResource(R.string.pet_talk_to, profile.name),
            role = Role.Button,
            onClick = onClick,
        )
        else -> modifier
    }
    Box(clickModifier) {
        Box(Modifier.fillMaxSize()) {
            if (showShadow) {
                Canvas(Modifier.fillMaxSize()) {
                    drawOval(
                        color = shadowColor,
                        topLeft = Offset(size.width * 0.24f, size.height * 0.88f),
                        size = Size(size.width * 0.52f, size.height * 0.08f),
                    )
                }
            }
            Box(
                Modifier.fillMaxSize()
                    .petCalmIdleAnimation(animateIdle && pose == PetPose.IDLE && !reacting)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 0.94f)
                        scaleX = poseScaleX * tapScaleX
                        scaleY = poseScaleY * tapScaleY
                        rotationZ = reaction.value * 1.5f + flightWobble * 2.5f
                        translationX = flightWobble * size.width * 0.008f
                    },
            ) {
                if (profile.species == PetSpecies.Hamster && hamsterAssets != null) {
                    HamsterPreview(
                        assets = hamsterAssets,
                        appearance = profile.hamsterAppearance,
                        modifier = Modifier.fillMaxSize(),
                        blink = hamsterBlink || pose == PetPose.LANDED || reaction.value > 0.7f,
                        mouthOpen = mouthOpen,
                        lookAt = lookAt,
                        flightPhase = flightPhase,
                        limbAmplitude = limbAmplitude,
                        clothingLayers = rememberClothingLayers(
                            profile.clothing.equippedBySlot,
                            profile.hamsterAppearance,
                        ),
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
}

private fun Modifier.hamsterClickable(
    assets: HamsterAssets,
    appearance: HamsterAppearance,
    blink: Boolean,
    contentDescription: String,
    onClick: (() -> Unit)?,
    gestureCallbacks: PetGestureCallbacks?,
    touchSlop: Float,
    longPressTimeoutMillis: Long,
    scaleX: Float,
    scaleY: Float,
): Modifier = this.then(
    HamsterClickableElement(
        assets = assets,
        appearance = appearance,
        blink = blink,
        onClick = onClick,
        gestureCallbacks = gestureCallbacks,
        touchSlop = touchSlop,
        longPressTimeoutMillis = longPressTimeoutMillis,
        scaleX = scaleX,
        scaleY = scaleY,
    ),
).semantics {
    this.contentDescription = contentDescription
    role = Role.Button
    if (onClick != null) {
        onClick {
            onClick()
            true
        }
    }
}.focusable()

/**
 * A transparent hamster sprite must not block a room object behind it.
 *
 * A regular [androidx.compose.ui.input.pointer.pointerInput] modifier still wins the sibling
 * hit-test for the whole square that contains the sprite, even when its gesture handler decides
 * not to consume the down event. This node opts into sibling sharing and consumes the gesture only
 * when the alpha mask says that the pointer is on an actual hamster pixel.
 */
private data class HamsterClickableElement(
    val assets: HamsterAssets,
    val appearance: HamsterAppearance,
    val blink: Boolean,
    val onClick: (() -> Unit)?,
    val gestureCallbacks: PetGestureCallbacks?,
    val touchSlop: Float,
    val longPressTimeoutMillis: Long,
    val scaleX: Float,
    val scaleY: Float,
) : ModifierNodeElement<HamsterClickableNode>() {
    override fun create(): HamsterClickableNode = HamsterClickableNode(
        assets = assets,
        appearance = appearance,
        blink = blink,
        onClick = onClick,
        gestureCallbacks = gestureCallbacks,
        touchSlop = touchSlop,
        longPressTimeoutMillis = longPressTimeoutMillis,
        scaleX = scaleX,
        scaleY = scaleY,
    )

    override fun update(node: HamsterClickableNode) {
        node.update(
            assets = assets,
            appearance = appearance,
            blink = blink,
            onClick = onClick,
            gestureCallbacks = gestureCallbacks,
            touchSlop = touchSlop,
            longPressTimeoutMillis = longPressTimeoutMillis,
            scaleX = scaleX,
            scaleY = scaleY,
        )
    }
}

private class HamsterClickableNode(
    private var assets: HamsterAssets,
    private var appearance: HamsterAppearance,
    private var blink: Boolean,
    private var onClick: (() -> Unit)?,
    private var gestureCallbacks: PetGestureCallbacks?,
    private var touchSlop: Float,
    private var longPressTimeoutMillis: Long,
    private var scaleX: Float,
    private var scaleY: Float,
) : Modifier.Node(), PointerInputModifierNode {
    private var pressedPointerId: androidx.compose.ui.input.pointer.PointerId? = null
    private var dragCallbacks: PetGestureCallbacks? = null
    private var dragging = false
    private var tapCancelled = false
    private var startRoot = Offset.Zero
    private var lastRoot = Offset.Zero
    private val recentPositions = ArrayDeque<Pair<Long, Offset>>()
    private var longPressJob: Job? = null

    override fun sharePointerInputWithSiblings(): Boolean = true

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Main || bounds.width <= 0 || bounds.height <= 0) return

        val containerSize = Size(bounds.width.toFloat(), bounds.height.toFloat())
        if (pressedPointerId == null) {
            val down = pointerEvent.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return
            if ((onClick != null || gestureCallbacks != null) &&
                assets.contains(appearance, unscale(down.position, containerSize), containerSize, blink)
            ) {
                pressedPointerId = down.id
                dragCallbacks = gestureCallbacks
                dragging = false
                tapCancelled = false
                startRoot = requireLayoutCoordinates().localToRoot(down.position)
                lastRoot = startRoot
                recentPositions.clear()
                record(down.uptimeMillis, startRoot)
                down.consume()
                dragCallbacks?.onTouchStart()
                if (dragCallbacks != null) {
                    longPressJob = coroutineScope.launch {
                        delay(longPressTimeoutMillis)
                        if (pressedPointerId == down.id && !dragging) {
                            dragging = true
                            dragCallbacks?.onGrab()
                        }
                    }
                }
            }
            return
        }

        val change = pointerEvent.changes.firstOrNull { it.id == pressedPointerId } ?: return
        val root = requireLayoutCoordinates().localToRoot(change.position)
        if (change.pressed) {
            record(change.uptimeMillis, root)
            if (!dragging && hypot(root.x - startRoot.x, root.y - startRoot.y) > touchSlop) {
                longPressJob?.cancel()
                longPressJob = null
                if (!tapCancelled) {
                    tapCancelled = true
                    dragCallbacks?.onCancel()
                }
            }
            if (dragging) {
                val delta = root - lastRoot
                if (delta != Offset.Zero) dragCallbacks?.onDrag(delta)
                change.consume()
            }
            lastRoot = root
        }
        if (change.changedToUpIgnoreConsumed()) {
            longPressJob?.cancel()
            longPressJob = null
            val wasDragging = dragging
            val release = if (wasDragging) velocity() else Offset.Zero
            pressedPointerId = null
            dragging = false
            recentPositions.clear()
            val callbacks = dragCallbacks
            dragCallbacks = null
            if (wasDragging) {
                change.consume()
                callbacks?.onRelease(release)
            } else {
                callbacks?.onCancel()
                if (!tapCancelled && !change.isConsumed && assets.contains(
                        appearance, unscale(change.position, containerSize), containerSize, blink,
                    )
                ) {
                    change.consume()
                    onClick?.invoke()
                }
            }
        }
    }

    override fun onCancelPointerInput() {
        longPressJob?.cancel()
        longPressJob = null
        dragCallbacks?.onCancel()
        pressedPointerId = null
        dragCallbacks = null
        dragging = false
        tapCancelled = false
        recentPositions.clear()
    }

    private fun unscale(position: Offset, size: Size): Offset = Offset(
        x = (position.x - size.width * 0.5f) / scaleX + size.width * 0.5f,
        y = (position.y - size.height * 0.94f) / scaleY + size.height * 0.94f,
    )

    private fun record(timeMillis: Long, position: Offset) {
        recentPositions.addLast(timeMillis to position)
        while (recentPositions.size > 2 && timeMillis - recentPositions.first().first > 120L) {
            recentPositions.removeFirst()
        }
    }

    private fun velocity(): Offset {
        val first = recentPositions.firstOrNull() ?: return Offset.Zero
        val last = recentPositions.lastOrNull() ?: return Offset.Zero
        val seconds = (last.first - first.first) / 1_000f
        return if (seconds > 0.015f) (last.second - first.second) / seconds else Offset.Zero
    }

    fun update(
        assets: HamsterAssets,
        appearance: HamsterAppearance,
        blink: Boolean,
        onClick: (() -> Unit)?,
        gestureCallbacks: PetGestureCallbacks?,
        touchSlop: Float,
        longPressTimeoutMillis: Long,
        scaleX: Float,
        scaleY: Float,
    ) {
        if (this.assets !== assets || this.appearance != appearance) {
            onCancelPointerInput()
        }
        this.assets = assets
        this.appearance = appearance
        this.blink = blink
        this.onClick = onClick
        this.gestureCallbacks = gestureCallbacks
        this.touchSlop = touchSlop
        this.longPressTimeoutMillis = longPressTimeoutMillis
        this.scaleX = scaleX
        this.scaleY = scaleY
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
        val assetManager = LocalResources.current.assets
        val bitmap by produceState<ImageBitmap?>(
            initialValue = null,
            assets,
            profile.hamsterAppearance,
            profile.clothing.equippedBySlot,
            blink,
            maxSidePx,
        ) {
            value = assets?.let {
                val clothes = ClothingArtwork.layers(
                    assetManager,
                    profile.clothing.equippedBySlot,
                    profile.hamsterAppearance,
                )
                withContext(Dispatchers.Default) {
                    it.renderBitmap(profile.hamsterAppearance, maxSidePx, blink, clothes).asImageBitmap()
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

@Preview(name = "Реакции питомца", widthDp = 330, heightDp = 130, showBackground = true)
@Composable
private fun PetMotionStatesPreview() {
    FinPetTheme {
        Row {
            listOf(PetPose.IDLE, PetPose.LANDED, PetPose.GETTING_UP).forEach { pose ->
                PetScene(
                    profile = PetProfile(name = "Финни", color = PetColor.Sunny),
                    modifier = Modifier.size(110.dp),
                    animateIdle = false,
                    pose = pose,
                    showShadow = pose == PetPose.IDLE,
                )
            }
        }
    }
}

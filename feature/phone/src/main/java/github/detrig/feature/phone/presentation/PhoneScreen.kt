package github.detrig.feature.phone.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.phone.PhoneFeature
import github.detrig.feature.phone.navigation.PhoneRoute
import github.detrig.feature.phone.R
import github.detrig.feature.shop.api.ShopApi
import github.detrig.products.GroceryStoreIds
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val CANVAS_WIDTH = 941f
private const val CANVAS_HEIGHT = 1672f
private const val SCREEN_LEFT = 85f
private const val SCREEN_TOP = 120f
private const val SCREEN_WIDTH = 772f
private const val SCREEN_HEIGHT = 1451f
private const val CLOSE_X = 748f
private const val CLOSE_Y = 160f
private const val BUTTON_SIZE = 80f
private const val PHONE_HEIGHT_EXTENSION = 1.22f
private const val PHONE_STRETCH_TOP = 160f
private const val PHONE_STRETCH_BOTTOM = 1320f

private const val GROCERY_APP = "grocery"
private const val CLOTHING_APP = "clothing"
private const val INTERIOR_APP = "interior"
private const val HOME_CLOSE_BUTTON_SIZE = 74f
private const val HOME_CLOSE_GLYPH_SIZE = 54f
private const val HOME_APP_ICON_Y = 310f
private const val HOME_APP_LABEL_Y = 499f

@Composable
internal fun PhoneScreen(route: PhoneRoute) {
    val component = PhoneFeature.component()
    var activeAppId by rememberSaveable(route) {
        mutableStateOf((route as? PhoneRoute.App)?.appId)
    }
    BackHandler(enabled = activeAppId != null) {
        activeAppId = null
    }
    component.petApi.RequirePet(modifier = Modifier.fillMaxSize()) { petProfile, _, _, _ ->
        Box(Modifier.fillMaxSize()) {
            component.roomApi.Content(
                modifier = Modifier.fillMaxSize(),
                active = false,
                canShowDialogs = false,
                petContent = { petModifier ->
                    component.petApi.Content(profile = petProfile, modifier = petModifier)
                },
            )
            PhoneDevice(
                route = route,
                activeAppId = activeAppId,
                shopApi = component.shopApi,
                onClose = { component.router.close() },
                onBack = { activeAppId = null },
                onOpenApp = { activeAppId = it },
            )
        }
    }
}

@Composable
private fun PhoneDevice(
    route: PhoneRoute,
    activeAppId: String?,
    shopApi: ShopApi,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onOpenApp: (String) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val scale = minOf(
            maxWidth.value / CANVAS_WIDTH,
            maxHeight.value / (CANVAS_HEIGHT * PHONE_HEIGHT_EXTENSION),
        )
        val stageWidth = (CANVAS_WIDTH * scale).dp
        val stageHeight = (CANVAS_HEIGHT * PHONE_HEIGHT_EXTENSION * scale).dp
        val entranceOffset = remember(route is PhoneRoute.Home) {
            Animatable(if (route is PhoneRoute.Home) 0.9f else 0f)
        }
        val entranceRotation = remember(route is PhoneRoute.Home) {
            Animatable(if (route is PhoneRoute.Home) 8f else 0f)
        }

        LaunchedEffect(route) {
            if (route !is PhoneRoute.Home) return@LaunchedEffect
            launch {
                entranceOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 760
                        0.9f at 0
                        0.34f at 230
                        -0.04f at 505
                        0f at 760
                    },
                )
            }
            entranceRotation.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 760
                    8f at 0
                    3f at 230
                    -2f at 505
                    0f at 760
                },
            )
        }

        Box(
            modifier = Modifier
                .size(stageWidth, stageHeight)
                .graphicsLayer {
                    translationY = size.height * entranceOffset.value
                    rotationZ = entranceRotation.value
                },
        ) {
            PhoneCanvasLayer(
                modifier = Modifier.fillMaxSize(),
            ) {
                val openAppId = activeAppId
                if (openAppId == null) {
                    PhoneHomeContent(
                        scale = scale,
                        onClose = onClose,
                        onOpenApp = onOpenApp,
                    )
                } else {
                    PhoneAppContent(
                        appId = openAppId,
                        scale = scale,
                        shopApi = shopApi,
                        onBack = onBack,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneCanvasLayer(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val assets = remember(context.resources) {
        PhoneCanvasAssets(
            wallpaper = ImageBitmap.imageResource(context.resources, R.drawable.phone_wallpaper_blue),
            screenMask = ImageBitmap.imageResource(context.resources, R.drawable.phone_screen_mask),
            overlay = ImageBitmap.imageResource(context.resources, R.drawable.phone_overlay),
        )
    }
    Box(modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawStretchablePhoneImage(
                        image = assets.screenMask,
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            StretchablePhoneImage(assets.wallpaper)
            content()
        }
        StretchablePhoneImage(assets.overlay)
    }
}

private data class PhoneCanvasAssets(
    val wallpaper: ImageBitmap,
    val screenMask: ImageBitmap,
    val overlay: ImageBitmap,
)

@Composable
private fun StretchablePhoneImage(
    image: ImageBitmap,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Canvas(modifier) {
        drawStretchablePhoneImage(image)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStretchablePhoneImage(
    image: ImageBitmap,
    blendMode: BlendMode = BlendMode.SrcOver,
) {
    val horizontalScale = size.width / CANVAS_WIDTH
    val verticalScale = size.height / (CANVAS_HEIGHT * horizontalScale)
    val extraHeight = CANVAS_HEIGHT * (verticalScale - 1f)
    val destinationTopEnd = (PHONE_STRETCH_TOP * horizontalScale).roundToInt()
    val destinationMiddleEnd = ((PHONE_STRETCH_BOTTOM + extraHeight) * horizontalScale).roundToInt()
    val sourceTopEnd = (PHONE_STRETCH_TOP / CANVAS_HEIGHT * image.height).roundToInt()
    val sourceMiddleEnd = (PHONE_STRETCH_BOTTOM / CANVAS_HEIGHT * image.height).roundToInt()

    fun drawSlice(
        sourceTop: Int,
        sourceBottom: Int,
        destinationTop: Int,
        destinationBottom: Int,
    ) {
        if (sourceBottom <= sourceTop || destinationBottom <= destinationTop) return
        drawImage(
            image = image,
            srcOffset = IntOffset(0, sourceTop),
            srcSize = IntSize(image.width, sourceBottom - sourceTop),
            dstOffset = IntOffset(0, destinationTop),
            dstSize = IntSize(size.width.toInt(), destinationBottom - destinationTop),
            blendMode = blendMode,
        )
    }

    drawSlice(0, sourceTopEnd, 0, destinationTopEnd)
    drawSlice(sourceTopEnd, sourceMiddleEnd, destinationTopEnd, destinationMiddleEnd)
    drawSlice(sourceMiddleEnd, image.height, destinationMiddleEnd, size.height.toInt())
}

@Composable
private fun PhoneHomeContent(
    scale: Float,
    onClose: () -> Unit,
    onOpenApp: (String) -> Unit,
) {
    PhoneAssetButton(
        x = CLOSE_X + (BUTTON_SIZE - HOME_CLOSE_BUTTON_SIZE),
        y = CLOSE_Y,
        scale = scale,
        glyph = R.drawable.phone_glyph_close,
        buttonSize = HOME_CLOSE_BUTTON_SIZE,
        glyphSize = HOME_CLOSE_GLYPH_SIZE,
        contentDescription = "Закрыть телефон",
        onClick = onClose,
    )
    PhoneStatusIcons(scale)
    val apps = listOf(
        PhoneAppVisual(R.drawable.phone_icon_grocery_hd, "Продуктовый", 129f, GROCERY_APP),
        PhoneAppVisual(R.drawable.phone_icon_clothing_hd, "Одежда", 382f, CLOTHING_APP),
        PhoneAppVisual(R.drawable.phone_icon_interior_hd, "Интерьер", 635f, INTERIOR_APP),
    )
    apps.forEach { app ->
        Image(
            painter = painterResource(app.iconRes),
            contentDescription = app.label,
            modifier = Modifier
                .offset(
                    x = (app.x * scale).dp,
                    y = (HOME_APP_ICON_Y * scale).dp,
                )
                .size((178f * scale).dp)
                .clickable(role = Role.Button, onClick = { onOpenApp(app.id) })
                .semantics { this.role = Role.Button },
            contentScale = ContentScale.Fit,
        )
        PhoneText(
            text = app.label,
            x = app.x + 89f,
            y = HOME_APP_LABEL_Y,
            width = 220f,
            scale = scale,
            fontSize = 28f,
            lineHeight = 34f,
            style = AppTheme.typography.bodyStrong,
        )
    }
}

private data class PhoneAppVisual(
    val iconRes: Int,
    val label: String,
    val x: Float,
    val id: String,
)

@Composable
private fun PhoneStatusIcons(scale: Float) {
    Row(
        modifier = Modifier
            .offset(x = (597f * scale).dp, y = (165f * scale).dp)
            .width((114f * scale).dp),
        horizontalArrangement = Arrangement.spacedBy((10f * scale).dp),
    ) {
        Image(
            painter = painterResource(R.drawable.phone_glyph_wifi),
            contentDescription = "Wi-Fi",
            modifier = Modifier.size((50f * scale).dp),
        )
        Image(
            painter = painterResource(R.drawable.phone_glyph_battery),
            contentDescription = "Заряд батареи",
            modifier = Modifier.size((54f * scale).dp),
        )
    }
}

@Composable
private fun PhoneAppContent(
    appId: String,
    scale: Float,
    shopApi: ShopApi,
    onBack: () -> Unit,
) {
    val screenTop = stretchedPhoneY(SCREEN_TOP)
    val screenBottom = stretchedPhoneY(SCREEN_TOP + SCREEN_HEIGHT)
    Box(
        modifier = Modifier
            .offset(x = (SCREEN_LEFT * scale).dp, y = (screenTop * scale).dp)
            .size((SCREEN_WIDTH * scale).dp, ((screenBottom - screenTop) * scale).dp)
            .background(AppTheme.colors.storefront.background)
            .clipToBounds(),
    ) {
        when (appId) {
            GROCERY_APP -> GroceryAppContent(shopApi = shopApi, onBack = onBack)
            CLOTHING_APP -> PhonePlaceholderApp(
                title = "Одежда",
                iconRes = R.drawable.phone_icon_clothing_hd,
                scale = scale,
                onBack = onBack,
            )
            INTERIOR_APP -> PhonePlaceholderApp(
                title = "Интерьер",
                iconRes = R.drawable.phone_icon_interior_hd,
                scale = scale,
                onBack = onBack,
            )
            else -> PhonePlaceholderApp(
                title = "Приложение",
                iconRes = R.drawable.phone_icon_tile_hd,
                scale = scale,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun GroceryAppContent(
    shopApi: ShopApi,
    onBack: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(PhoneShopPage.Catalog) }
    when (page) {
        PhoneShopPage.Catalog -> shopApi.Content(
            storeId = GroceryStoreIds.Store,
            onBack = onBack,
            onOpenCart = { page = PhoneShopPage.Cart },
            closeAfterReceipt = onBack,
        )
        PhoneShopPage.Cart -> shopApi.CartContent(
            storeId = GroceryStoreIds.Store,
            onBack = { page = PhoneShopPage.Catalog },
            onCheckoutCompleted = { page = PhoneShopPage.Catalog },
        )
    }
}

private enum class PhoneShopPage { Catalog, Cart }

@Composable
private fun PhonePlaceholderApp(
    title: String,
    iconRes: Int,
    scale: Float,
    onBack: () -> Unit,
) {
    PhoneAssetButton(
        x = 110f,
        y = 158f,
        scale = scale,
        glyph = R.drawable.phone_glyph_back,
        contentDescription = "Назад",
        onClick = onBack,
    )
    Image(
        painter = painterResource(iconRes),
        contentDescription = title,
        modifier = Modifier
            .offset(
                x = (((SCREEN_WIDTH - 178f) / 2f) * scale).dp,
                y = (300f * scale).dp,
            )
            .size((178f * scale).dp),
    )
    PhoneText(
        text = title,
        x = SCREEN_WIDTH / 2f,
        y = 510f,
        width = 600f,
        scale = scale,
        fontSize = 40f,
        lineHeight = 48f,
        style = AppTheme.typography.brand,
    )
    PhoneText(
        text = "Раздел откроется внутри телефона",
        x = SCREEN_WIDTH / 2f,
        y = 610f,
        width = 600f,
        scale = scale,
        fontSize = 24f,
        lineHeight = 30f,
        style = AppTheme.typography.body,
    )
}

@Composable
private fun PhoneAssetButton(
    x: Float,
    y: Float,
    scale: Float,
    glyph: Int,
    contentDescription: String,
    onClick: () -> Unit,
    buttonSize: Float = BUTTON_SIZE,
    glyphSize: Float = 64f,
) {
    val visualSize = (buttonSize * scale).dp
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .offset(x = (x * scale).dp, y = (y * scale).dp)
            .sizeIn(minWidth = AppTheme.sizes.minimumTouchTarget, minHeight = AppTheme.sizes.minimumTouchTarget)
            .size(visualSize)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(
                if (isPressed) {
                    R.drawable.phone_button_square_pressed
                } else {
                    R.drawable.phone_button_square_normal
                },
            ),
            contentDescription = null,
            modifier = Modifier.size(visualSize),
        )
        Image(
            painter = painterResource(glyph),
            contentDescription = null,
            modifier = Modifier.size((glyphSize * scale).dp),
        )
    }
}

@Composable
private fun PhoneText(
    text: String,
    x: Float,
    y: Float,
    width: Float,
    scale: Float,
    fontSize: Float,
    lineHeight: Float,
    style: androidx.compose.ui.text.TextStyle,
) {
    Text(
        text = text,
        modifier = Modifier
            .offset(
                x = ((x - width / 2f) * scale).dp,
                y = (y * scale).dp,
            )
            .width((width * scale).dp),
        color = AppTheme.colors.storefront.onSurface,
        style = style.copy(
            fontSize = (fontSize * scale).sp,
            lineHeight = (lineHeight * scale).sp,
        ),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun stretchedPhoneY(y: Float): Float {
    val extraHeight = CANVAS_HEIGHT * (PHONE_HEIGHT_EXTENSION - 1f)
    return when {
        y <= PHONE_STRETCH_TOP -> y
        y >= PHONE_STRETCH_BOTTOM -> y + extraHeight
        else -> PHONE_STRETCH_TOP + (y - PHONE_STRETCH_TOP) * PHONE_HEIGHT_EXTENSION
    }
}

@Preview(name = "Телефон", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun PhonePreview() {
    FinPetTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.roomBackground),
            contentAlignment = Alignment.BottomCenter,
        ) {
            PhoneCanvasLayer(
                modifier = Modifier.size(360.dp, (639.4f * PHONE_HEIGHT_EXTENSION).dp),
            ) {
                PhoneHomeContent(
                    scale = 360f / CANVAS_WIDTH,
                    onClose = {},
                    onOpenApp = {},
                )
            }
        }
    }
}

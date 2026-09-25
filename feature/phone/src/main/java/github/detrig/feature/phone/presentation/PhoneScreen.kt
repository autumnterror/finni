package github.detrig.feature.phone.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.layout.positionInParent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetOutlinedButton
import github.detrig.designsystem.component.FinPetDialogueAction
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.phone.PhoneFeature
import github.detrig.feature.phone.api.MESSAGES_APP_ID
import github.detrig.feature.phone.navigation.PhoneRoute
import github.detrig.feature.phone.R
import github.detrig.feature.shop.api.ShopApi
import github.detrig.products.GroceryStoreIds
import github.detrig.products.GroceryCatalog
import github.detrig.products.ProductId
import github.detrig.feature.room.api.FirstRunOnboardingStep
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
private const val DEBUG_APP = "debug"
private const val HOME_CLOSE_BUTTON_SIZE = 74f
private const val HOME_CLOSE_GLYPH_SIZE = 54f

@Composable
internal fun PhoneScreen(route: PhoneRoute) {
    val component = PhoneFeature.component()
    val componentContext = LocalContext.current
    val messagesViewModel: MessagesViewModel = viewModel { component.messagesViewModel() }
    val messagesState by messagesViewModel.state().observeAsState(MessagesViewState())
    val firstRunStep by component.roomApi.firstRunGuide.step.collectAsState()
    var activeAppId by rememberSaveable(route) {
        mutableStateOf((route as? PhoneRoute.App)?.appId)
    }
    LaunchedEffect(messagesViewModel) { messagesViewModel.perform(MessagesViewEvent.Load) }
    LaunchedEffect(activeAppId) {
        if (activeAppId == MESSAGES_APP_ID) {
            messagesViewModel.perform(MessagesViewEvent.AppOpened)
        }
    }
    BackHandler(
        enabled = activeAppId != null || firstRunStep in firstRunPhoneHomeSteps,
    ) {
        if (activeAppId != null && firstRunStep !in firstRunShopLockedSteps) {
            activeAppId = null
        }
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
                messagesState = messagesState,
                onMessagesEvent = messagesViewModel::perform,
                onClose = {
                    if (firstRunStep !in firstRunPhoneHomeSteps) component.router.close()
                },
                onBack = {
                    if (firstRunStep !in firstRunShopLockedSteps) activeAppId = null
                },
                onOpenApp = { appId ->
                    if (firstRunStep == FirstRunOnboardingStep.WAITING_FOR_STORE) {
                        if (appId == GROCERY_APP) {
                            component.roomApi.firstRunGuide.moveTo(
                                FirstRunOnboardingStep.SHOP_PRICE_GUIDANCE,
                            )
                            activeAppId = appId
                        }
                    } else {
                        activeAppId = appId
                    }
                },
                firstRunStep = firstRunStep,
                onFirstRunProductSelected = {
                    component.roomApi.firstRunGuide.moveTo(
                        FirstRunOnboardingStep.SHOP_FOOD_SELECTED,
                    )
                },
                onFirstRunCheckout = { spentRub ->
                    if (component.roomApi.firstRunGuide.step.value in firstRunShopPurchaseSteps) {
                        component.roomApi.firstRunGuide.moveTo(FirstRunOnboardingStep.PURCHASE_READY)
                        component.globalMessageController.showMessage(
                            componentContext.getString(R.string.first_run_spent, spentRub),
                        )
                    }
                },
            )
            if (activeAppId == MESSAGES_APP_ID) {
                MessagesPetDialogue(
                    state = messagesState,
                    portrait = { modifier ->
                        component.petApi.Portrait(profile = petProfile, modifier = modifier)
                    },
                    onGuidanceDismissed = { eventId ->
                        messagesViewModel.perform(MessagesViewEvent.GuidanceDismissed(eventId))
                    },
                    onFeedbackDismissed = { eventId ->
                        messagesViewModel.perform(MessagesViewEvent.FeedbackDismissed(eventId))
                    },
                )
            }
            when {
                firstRunStep == FirstRunOnboardingStep.PHONE_STORE_GUIDANCE && activeAppId == null -> {
                    FinPetDialogueDialog(
                        speakerName = petProfile.name,
                        cards = listOf(componentContext.getString(R.string.first_run_store_intro)),
                        portrait = { modifier -> component.petApi.Portrait(petProfile, modifier) },
                        dismissOnBackPress = false,
                        onFinished = {
                            component.roomApi.firstRunGuide.moveTo(
                                FirstRunOnboardingStep.WAITING_FOR_STORE,
                            )
                        },
                    )
                }
                firstRunStep == FirstRunOnboardingStep.SHOP_PRICE_GUIDANCE && activeAppId == GROCERY_APP -> {
                    FinPetDialogueDialog(
                        speakerName = petProfile.name,
                        cards = listOf(componentContext.getString(R.string.first_run_price_intro)),
                        portrait = { modifier -> component.petApi.Portrait(petProfile, modifier) },
                        advanceOnTap = false,
                        dismissOnBackPress = false,
                        actions = listOf(FinPetDialogueAction("next", componentContext.getString(R.string.first_run_next))),
                        onActionSelected = {
                            component.roomApi.firstRunGuide.moveTo(FirstRunOnboardingStep.SHOP_FOOD_GUIDANCE)
                        },
                        onFinished = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneDevice(
    route: PhoneRoute,
    activeAppId: String?,
    shopApi: ShopApi,
    messagesState: MessagesViewState,
    onMessagesEvent: (MessagesViewEvent) -> Unit,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onOpenApp: (String) -> Unit,
    firstRunStep: FirstRunOnboardingStep,
    onFirstRunProductSelected: () -> Unit,
    onFirstRunCheckout: (Long) -> Unit,
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
                        unreadMessages = messagesState.inbox.unreadCount,
                        onClose = onClose,
                        onOpenApp = onOpenApp,
                        highlightedAppId = GROCERY_APP.takeIf {
                            firstRunStep == FirstRunOnboardingStep.PHONE_STORE_GUIDANCE ||
                                firstRunStep == FirstRunOnboardingStep.WAITING_FOR_STORE
                        },
                    )
                } else {
                    PhoneAppContent(
                        appId = openAppId,
                        scale = scale,
                        shopApi = shopApi,
                        messagesState = messagesState,
                        onMessagesEvent = onMessagesEvent,
                        onBack = onBack,
                        firstRunStep = firstRunStep,
                        onFirstRunProductSelected = onFirstRunProductSelected,
                        onFirstRunCheckout = onFirstRunCheckout,
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
    unreadMessages: Int,
    onClose: () -> Unit,
    onOpenApp: (String) -> Unit,
    highlightedAppId: String? = null,
) {
    var highlightedAppBounds by remember { mutableStateOf<Rect?>(null) }
    LaunchedEffect(highlightedAppId) { highlightedAppBounds = null }
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
        PhoneAppVisual(R.drawable.phone_icon_grocery_hd, "Продуктовый", 129f, 310f, GROCERY_APP),
        PhoneAppVisual(R.drawable.phone_icon_clothing_hd, "Одежда", 382f, 310f, CLOTHING_APP),
        PhoneAppVisual(R.drawable.phone_icon_interior_hd, "Интерьер", 635f, 310f, INTERIOR_APP),
        PhoneAppVisual(R.drawable.phone_icon_messages, "Сообщения", 129f, 620f, MESSAGES_APP_ID),
        PhoneAppVisual(R.drawable.phone_icon_tile_hd, "Дебаг меню", 382f, 620f, DEBUG_APP),
    )
    apps.forEach { app ->
        Box(
            modifier = Modifier
                .offset(
                    x = (app.x * scale).dp,
                    y = (app.y * scale).dp,
                )
                .size((178f * scale).dp)
                .then(
                    if (app.id == highlightedAppId) Modifier
                        .border(4.dp, AppTheme.colors.actionPrimary, AppTheme.shapes.storefrontControl)
                        .padding(4.dp)
                    else Modifier,
                )
                .onGloballyPositioned { coordinates ->
                    if (app.id == highlightedAppId) {
                        val position = coordinates.positionInParent()
                        val bounds = Rect(
                            left = position.x,
                            top = position.y,
                            right = position.x + coordinates.size.width,
                            bottom = position.y + coordinates.size.height,
                        )
                        if (highlightedAppBounds != bounds) highlightedAppBounds = bounds
                    }
                }
                .clickable(role = Role.Button, onClick = { onOpenApp(app.id) })
                .semantics { this.role = Role.Button },
        ) {
            Image(
                painter = painterResource(app.iconRes),
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            if (app.id == MESSAGES_APP_ID && unreadMessages > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size((44f * scale).dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(AppTheme.colors.statusCritical.accent)
                        .semantics {
                            contentDescription = "Непрочитанных сообщений: $unreadMessages"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = unreadMessages.coerceAtMost(9).toString(),
                        color = AppTheme.colors.statusCritical.onContainer,
                        style = AppTheme.typography.label,
                    )
                }
            }
        }
        PhoneText(
            text = app.label,
            x = app.x + 89f,
            y = app.y + 189f,
            width = 220f,
            scale = scale,
            fontSize = 28f,
            lineHeight = 34f,
            style = AppTheme.typography.bodyStrong,
        )
    }
    highlightedAppBounds?.let { bounds ->
        PhoneAppTutorialMask(bounds = bounds)
    }
}

@Composable
private fun PhoneAppTutorialMask(
    bounds: Rect,
) {
    val scrimColor = AppTheme.colors.sceneShadow.copy(alpha = 0.76f)
    val outlineColor = AppTheme.colors.actionPrimary
    val density = LocalDensity.current
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val padding = with(density) { 16.dp.toPx() }
        val topLeft = Offset(
            x = bounds.left - padding,
            y = bounds.top - padding,
        )
        val spotlightSize = Size(
            width = bounds.width + padding * 2,
            height = bounds.height + padding * 2,
        )
        val cornerRadius = CornerRadius(with(density) { 28.dp.toPx() })
        drawRect(scrimColor)
        drawRoundRect(
            color = outlineColor,
            topLeft = topLeft,
            size = spotlightSize,
            cornerRadius = cornerRadius,
            blendMode = BlendMode.Clear,
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = topLeft,
            size = spotlightSize,
            cornerRadius = cornerRadius,
            style = Stroke(width = with(density) { 4.dp.toPx() }),
        )
    }
}

private data class PhoneAppVisual(
    val iconRes: Int,
    val label: String,
    val x: Float,
    val y: Float,
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
    messagesState: MessagesViewState,
    onMessagesEvent: (MessagesViewEvent) -> Unit,
    onBack: () -> Unit,
    firstRunStep: FirstRunOnboardingStep,
    onFirstRunProductSelected: () -> Unit,
    onFirstRunCheckout: (Long) -> Unit,
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
            GROCERY_APP -> GroceryAppContent(
                shopApi = shopApi,
                onBack = onBack,
                firstRunStep = firstRunStep,
                onFirstRunProductSelected = onFirstRunProductSelected,
                onFirstRunCheckout = onFirstRunCheckout,
            )
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
            MESSAGES_APP_ID -> MessagesApp(
                state = messagesState,
                onBack = onBack,
                onThreadOpened = { onMessagesEvent(MessagesViewEvent.ThreadOpened(it)) },
                onThreadClosed = { onMessagesEvent(MessagesViewEvent.ThreadClosed) },
                onSuspiciousInteraction = { eventId, choice ->
                    onMessagesEvent(MessagesViewEvent.SuspiciousInteraction(eventId, choice))
                },
                onParentHelpOfferOpened = { onMessagesEvent(MessagesViewEvent.ParentHelpOfferOpened) },
                onParentHelpAccepted = { onMessagesEvent(MessagesViewEvent.ParentHelpAccepted(it)) },
                onParentHelpPaidOff = { onMessagesEvent(MessagesViewEvent.ParentHelpPaidOff) },
                onParentHelpDismissed = { onMessagesEvent(MessagesViewEvent.ParentHelpDismissed) },
            )
            DEBUG_APP -> DebugMenuApp(onBack = onBack)
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
private fun DebugMenuApp(onBack: () -> Unit) {
    val viewModel: DebugMenuViewModel = viewModel {
        PhoneFeature.component().debugMenuViewModel()
    }
    val state by viewModel.state().observeAsState(DebugMenuViewState())
    LaunchedEffect(viewModel) { viewModel.perform(DebugMenuViewEvent.Load) }

    DebugMenuContent(
        state = state,
        onBack = onBack,
        onChangeBalance = { viewModel.perform(DebugMenuViewEvent.ChangeBalance(it)) },
        onResetBalance = { viewModel.perform(DebugMenuViewEvent.ResetBalance) },
        onEndWeek = { viewModel.perform(DebugMenuViewEvent.EndWeek) },
    )
}

@Composable
private fun DebugMenuContent(
    state: DebugMenuViewState,
    onBack: () -> Unit,
    onChangeBalance: (Long) -> Unit,
    onResetBalance: () -> Unit,
    onEndWeek: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        FinPetOutlinedButton(
            text = "Назад",
            onClick = onBack,
            style = FinPetButtonDefaults.storefrontOutlinedStyle(),
        )
        Text(
            text = "Дебаг меню",
            style = AppTheme.typography.screenTitle,
            color = AppTheme.colors.storefront.onSurface,
        )
        FinPetCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(AppTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                Text("Текущий баланс", style = AppTheme.typography.body)
                Text(
                    text = "${state.balanceRub} ₽",
                    style = AppTheme.typography.currency,
                    color = AppTheme.colors.currencyAccent,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            DebugBalanceButton("−100 ₽", -100, state, onChangeBalance, Modifier.weight(1f))
            DebugBalanceButton("+100 ₽", 100, state, onChangeBalance, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            DebugBalanceButton("−10 ₽", -10, state, onChangeBalance, Modifier.weight(1f))
            DebugBalanceButton("+10 ₽", 10, state, onChangeBalance, Modifier.weight(1f))
        }
        FinPetOutlinedButton(
            text = "Обнулить баланс",
            onClick = onResetBalance,
            enabled = !state.isChanging && !state.isEndingWeek && state.balanceRub > 0,
            modifier = Modifier.fillMaxWidth(),
            style = FinPetButtonDefaults.storefrontOutlinedStyle(),
        )
        FinPetButton(
            text = if (state.isEndingWeek) "Завершаем неделю…" else "Завершить неделю",
            onClick = onEndWeek,
            enabled = !state.isChanging && !state.isEndingWeek,
            modifier = Modifier.fillMaxWidth(),
            style = FinPetButtonDefaults.storefrontPrimaryStyle(),
        )
        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.statusCritical.accent,
            )
        }
        state.statusMessage?.let { message ->
            Text(
                text = message,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.storefront.onSurface,
            )
        }
    }
}

@Composable
private fun DebugBalanceButton(
    label: String,
    deltaRub: Long,
    state: DebugMenuViewState,
    onChangeBalance: (Long) -> Unit,
    modifier: Modifier,
) {
    FinPetButton(
        text = label,
        onClick = { onChangeBalance(deltaRub) },
        enabled = !state.isChanging && !state.isEndingWeek &&
            (deltaRub > 0 || state.balanceRub >= -deltaRub),
        modifier = modifier,
        style = FinPetButtonDefaults.storefrontPrimaryStyle(),
    )
}

@Composable
private fun GroceryAppContent(
    shopApi: ShopApi,
    onBack: () -> Unit,
    firstRunStep: FirstRunOnboardingStep,
    onFirstRunProductSelected: () -> Unit,
    onFirstRunCheckout: (Long) -> Unit,
) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(PhoneShopPage.Catalog) }
    val highlightedProductId = remember {
        GroceryCatalog().storefront.items.minByOrNull { it.priceRub }?.id
    }
    when (page) {
        PhoneShopPage.Catalog -> shopApi.Content(
            storeId = GroceryStoreIds.Store,
            onBack = onBack,
            onOpenCart = { page = PhoneShopPage.Cart },
            closeAfterReceipt = onBack,
            highlightedProductId = highlightedProductId.takeIf {
                firstRunStep == FirstRunOnboardingStep.SHOP_FOOD_GUIDANCE
            },
            onProductSelected = { productId ->
                if (firstRunStep == FirstRunOnboardingStep.SHOP_FOOD_GUIDANCE &&
                    productId == highlightedProductId
                ) {
                    onFirstRunProductSelected()
                }
            },
            tutorialMessage = if (firstRunStep == FirstRunOnboardingStep.SHOP_FOOD_GUIDANCE) {
                context.getString(R.string.first_run_buy_food)
            } else null,
        )
        PhoneShopPage.Cart -> shopApi.CartContent(
            storeId = GroceryStoreIds.Store,
            onBack = { page = PhoneShopPage.Catalog },
            onCheckoutCompleted = { spentRub ->
                onFirstRunCheckout(spentRub)
                page = PhoneShopPage.Catalog
            },
        )
    }
}

private enum class PhoneShopPage { Catalog, Cart }

private val firstRunPhoneHomeSteps = setOf(
    FirstRunOnboardingStep.PHONE_STORE_GUIDANCE,
    FirstRunOnboardingStep.WAITING_FOR_STORE,
)

private val firstRunShopLockedSteps = setOf(
    FirstRunOnboardingStep.SHOP_PRICE_GUIDANCE,
    FirstRunOnboardingStep.SHOP_FOOD_GUIDANCE,
    FirstRunOnboardingStep.SHOP_FOOD_SELECTED,
)

private val firstRunShopPurchaseSteps = setOf(
    FirstRunOnboardingStep.SHOP_FOOD_GUIDANCE,
    FirstRunOnboardingStep.SHOP_FOOD_SELECTED,
)

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
                    unreadMessages = 2,
                    onClose = {},
                    onOpenApp = {},
                )
            }
        }
    }
}

@Preview(name = "Дебаг меню", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun DebugMenuPreview() {
    FinPetTheme {
        Box(Modifier.background(AppTheme.colors.storefront.background)) {
            DebugMenuContent(
                state = DebugMenuViewState(balanceRub = 350),
                onBack = {},
                onChangeBalance = {},
                onResetBalance = {},
                onEndWeek = {},
            )
        }
    }
}

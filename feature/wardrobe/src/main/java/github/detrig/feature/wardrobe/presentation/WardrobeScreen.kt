package github.detrig.feature.wardrobe.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.component.FinPetBackButton
import github.detrig.designsystem.component.FinPetStorefrontHeader
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetCoinText
import github.detrig.designsystem.component.FinPetFilterChipRow
import github.detrig.designsystem.component.FinPetFilterChipDefaults
import github.detrig.designsystem.component.FinPetGridColumns
import github.detrig.designsystem.component.FinPetLazyGrid
import github.detrig.designsystem.component.FinPetStorefrontCard
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.pet.api.ClothingItem
import github.detrig.feature.pet.domain.model.ClothingState
import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.presentation.PetScene
import github.detrig.feature.room.R as RoomR
import github.detrig.feature.wardrobe.WardrobeFeature

private val categories = listOf(
    null to "Все",
    "body" to "Одежда",
    "head" to "На голову",
    "neck" to "На шею",
    "face" to "Очки",
    "back" to "Аксессуары",
)

@Composable
internal fun WardrobeScreen(
    mode: WardrobeMode,
    onBack: (() -> Unit)? = null,
) {
    val component = WardrobeFeature.component()
    val viewModel: WardrobeViewModel = viewModel(key = "wardrobe-${mode.name}") {
        component.viewModel(mode)
    }
    val state by viewModel.state().observeAsState(viewModel.state().value ?: WardrobeViewState())
    val pet = component.petApi
    LaunchedEffect(viewModel) { viewModel.perform(WardrobeViewEvent.Load) }
    val navigateBack = { onBack?.invoke() ?: viewModel.perform(WardrobeViewEvent.Back) }
    BackHandler(onBack = navigateBack)
    WardrobeContent(
        state = state,
        onEvent = viewModel::perform,
        onBack = navigateBack,
        useSafeInsets = mode == WardrobeMode.OWNED,
        thumbnail = { id, modifier -> pet.ClothingThumbnail(id, modifier) },
        petPreview = { profile, outfit, modifier -> pet.OutfitPreview(profile, outfit, modifier) },
    )
}

@Composable
internal fun WardrobeContent(
    state: WardrobeViewState,
    onEvent: (WardrobeViewEvent) -> Unit,
    onBack: () -> Unit,
    useSafeInsets: Boolean = true,
    thumbnail: @Composable (String, Modifier) -> Unit,
    petPreview: @Composable (PetProfile, Map<String, String>, Modifier) -> Unit,
) {
    val colors = AppTheme.colors.storefront
    val gridState = key(state.mode, state.category, state.profile?.clothing?.ownedIds.orEmpty()) {
        // Start each list at its beginning instead of following an owned item
        // when switching categories or moving a new purchase to the bottom.
        rememberLazyGridState()
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .then(
                if (useSafeInsets) {
                    Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                } else {
                    Modifier
                },
            ),
    ) {
            val stageHeight = (maxHeight * 0.29f).coerceIn(175.dp, 250.dp)
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WardrobeHeader(state = state, onBack = onBack)
                FittingRoom(
                    profile = state.profile,
                    outfit = state.previewOutfit,
                    trial = state.isTrial,
                    onClearTrial = { onEvent(WardrobeViewEvent.ClearTrial) },
                    petPreview = petPreview,
                    modifier = Modifier.fillMaxWidth().height(stageHeight),
                )
                FinPetFilterChipRow(
                    items = categories,
                    isSelected = { it.first == state.category },
                    onItemSelected = { onEvent(WardrobeViewEvent.CategorySelected(it.first)) },
                    text = { it.second },
                    key = { it.first ?: "all" },
                    style = FinPetFilterChipDefaults.storefrontCompactStyle(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.loading) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primaryAction)
                    }
                } else if (state.visibleItems.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (state.mode == WardrobeMode.OWNED) {
                                "Пока здесь нет купленных вещей"
                            } else {
                                "Нет вещей в этой категории"
                            },
                            style = AppTheme.typography.body,
                            color = colors.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    FinPetLazyGrid(
                        items = state.visibleItems,
                        state = gridState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        columns = FinPetGridColumns.Fixed(3),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        horizontalSpacing = 8.dp,
                        verticalSpacing = 10.dp,
                        key = ClothingItem::id,
                    ) { item ->
                        ClothingCard(
                            item = item,
                            owned = item.id in state.profile?.clothing?.ownedIds.orEmpty(),
                            equipped = state.profile?.clothing?.equippedBySlot?.get(item.slot) == item.id,
                            selected = state.selectedId == item.id,
                            thumbnail = thumbnail,
                            onClick = { onEvent(WardrobeViewEvent.ItemSelected(item.id)) },
                        )
                    }
                }
                WardrobeActionBar(state, onEvent)
            }
    }
}

@Composable
private fun WardrobeHeader(state: WardrobeViewState, onBack: () -> Unit) {
    if (state.mode == WardrobeMode.SHOP) {
        FinPetStorefrontHeader(
            title = "Одежда",
            balanceRub = state.balanceRub,
            onBack = onBack,
            backContentDescription = "Вернуться к приложениям",
        )
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FinPetBackButton(onClick = onBack, contentDescription = "Вернуться в комнату")
            Text(
                "Гардероб",
                modifier = Modifier.weight(1f),
                style = AppTheme.typography.screenTitle,
                color = AppTheme.colors.storefront.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(Modifier.size(AppTheme.sizes.preferredTouchTarget))
        }
    }
}

@Composable
private fun FittingRoom(
    profile: PetProfile?,
    outfit: Map<String, String>,
    trial: Boolean,
    onClearTrial: () -> Unit,
    petPreview: @Composable (PetProfile, Map<String, String>, Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors.storefront
    val house = AppTheme.colors.house
    FinPetStorefrontCard(modifier = modifier) {
        BoxWithConstraints(Modifier.fillMaxSize().clip(AppTheme.shapes.storefrontControl)) {
            val stageHeight = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                drawRect(color = house.hallWall)
                val stripe = size.width / 14f
                repeat(14) { index ->
                    if (index % 2 == 1) drawRect(
                        color = house.hallStripe,
                        topLeft = androidx.compose.ui.geometry.Offset(index * stripe, 0f),
                        size = androidx.compose.ui.geometry.Size(stripe, size.height * 0.83f),
                    )
                }
                drawRect(
                    color = house.woodLight,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.83f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.17f),
                )
            }
            Image(
                painter = painterResource(RoomR.drawable.room_rug_bedroom),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.48f).height(stageHeight * 0.18f),
                contentScale = ContentScale.Fit,
            )
            Image(
                painter = painterResource(RoomR.drawable.room_wardrobe),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 5.dp, bottom = 7.dp)
                    .size(width = (maxWidth * 0.25f).coerceAtMost(100.dp), height = stageHeight * 0.72f),
                contentScale = ContentScale.Fit,
            )
            Image(
                painter = painterResource(RoomR.drawable.room_standing_mirror),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 6.dp)
                    .size(width = (maxWidth * 0.18f).coerceAtMost(70.dp), height = stageHeight * 0.62f),
                contentScale = ContentScale.Fit,
            )
            profile?.let {
                petPreview(
                    it,
                    outfit,
                    Modifier.align(Alignment.BottomCenter)
                        .size((stageHeight * 1.08f).coerceAtMost(maxWidth * 0.68f)),
                )
            }
            if (trial) {
                Text(
                    "Примерка",
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        .background(colors.surface, AppTheme.shapes.badge).padding(horizontal = 8.dp, vertical = 4.dp),
                    style = AppTheme.typography.caption,
                    color = colors.onSurface,
                )
                FinPetButton(
                    text = "×",
                    onClick = onClearTrial,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(40.dp)
                        .semantics { contentDescription = "Закрыть примерку" },
                    style = FinPetButtonDefaults.storefrontOutlinedStyle().copy(
                        minHeight = 40.dp,
                        contentPadding = PaddingValues(0.dp),
                    ),
                )
            }
        }
    }
}

@Composable
private fun ClothingCard(
    item: ClothingItem,
    owned: Boolean,
    equipped: Boolean,
    selected: Boolean,
    thumbnail: @Composable (String, Modifier) -> Unit,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val captionLineHeight = with(LocalDensity.current) { AppTheme.typography.caption.lineHeight.toDp() }
    val titleHeight = captionLineHeight * 2 + AppTheme.spacing.xs
    val badgeHeight = maxOf(25.dp, captionLineHeight + AppTheme.spacing.xs)
    val nameStyle = AppTheme.typography.caption.let { base ->
        if (item.name.split(' ').maxOf(String::length) >= 14) {
            base.copy(fontSize = base.fontSize * 0.80f)
        } else base
    }
    FinPetStorefrontCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        containerColor = if (selected) colors.storefront.selectedSurface else colors.storefront.surface,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .height(71.dp + titleHeight + badgeHeight + 10.dp + AppTheme.spacing.sm)
                .padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(Modifier.fillMaxWidth().height(71.dp), contentAlignment = Alignment.Center) {
                thumbnail(item.id, Modifier.fillMaxSize())
            }
            Text(
                item.name,
                modifier = Modifier.fillMaxWidth().height(titleHeight),
                style = nameStyle,
                color = colors.storefront.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                Modifier.fillMaxWidth().height(badgeHeight)
                    .background(
                        if (owned) colors.storefront.primaryAction.copy(alpha = 0.35f) else colors.currencyContainer,
                        AppTheme.shapes.badge,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (owned) {
                    Text(
                        if (equipped) "Надето" else "Куплено",
                        style = AppTheme.typography.caption,
                        color = colors.storefront.onSurface,
                        maxLines = 1,
                    )
                } else {
                    FinPetCoinText(
                        "${item.priceRub} ₽",
                        style = AppTheme.typography.caption,
                        color = colors.storefront.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun WardrobeActionBar(state: WardrobeViewState, onEvent: (WardrobeViewEvent) -> Unit) {
    val item = state.selectedItem
    val owned = item?.id in state.profile?.clothing?.ownedIds.orEmpty()
    val equipped = item != null && state.profile?.clothing?.equippedBySlot?.get(item.slot) == item.id
    val enabled = when (state.mode) {
        WardrobeMode.OWNED -> item != null && owned && !state.purchasing
        WardrobeMode.SHOP -> item != null && !owned && !state.purchasing && state.balanceRub >= item.priceRub
    }
    val action = when {
        item == null -> "Выберите вещь"
        state.purchasing -> "Покупаем…"
        state.mode == WardrobeMode.SHOP && owned -> "Уже куплено"
        equipped -> "Снять"
        owned -> "Надеть"
        state.balanceRub < item.priceRub -> "Не хватает монет · ${item.priceRub} ₽"
        else -> "Купить и надеть · ${item.priceRub} ₽"
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        state.message?.let { message ->
            Text(
                message,
                modifier = Modifier.fillMaxWidth().clickable { onEvent(WardrobeViewEvent.DismissMessage) },
                style = AppTheme.typography.caption,
                color = AppTheme.colors.storefront.onSurface,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                item?.name ?: "",
                modifier = Modifier.weight(1f),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.storefront.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.mode == WardrobeMode.SHOP && item != null && !owned) {
                Text(
                    "Примерка бесплатна",
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
        if (item != null && !owned && state.balanceRub >= item.priceRub) {
            FinPetCoinText(
                "После покупки: ${state.balanceRub - item.priceRub} ₽ · до +${github.detrig.feature.wardrobe.domain.ClothingHappinessRewards.points(item)} счастья",
                style = AppTheme.typography.caption,
                color = AppTheme.colors.storefront.onSurface,
            )
        }
        FinPetButton(
            text = action,
            onClick = { onEvent(WardrobeViewEvent.PrimaryAction) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            style = FinPetButtonDefaults.storefrontPrimaryStyle(),
        )
    }
}

@Preview(name = "Одежда", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun WardrobePreview(
    @PreviewParameter(WardrobePreviewProvider::class) state: WardrobeViewState,
) {
    FinPetTheme {
        WardrobeContent(
            state = state,
            onEvent = {},
            onBack = {},
            thumbnail = { _, modifier -> Spacer(modifier) },
            petPreview = { pet, outfit, modifier ->
                PetScene(
                    profile = pet.copy(clothing = pet.clothing.copy(equippedBySlot = outfit)),
                    modifier = modifier,
                    animateIdle = false,
                    showShadow = false,
                )
            },
        )
    }
}

private class WardrobePreviewProvider : PreviewParameterProvider<WardrobeViewState> {
    private val items = listOf(
        ClothingItem("31_round_glasses", "Круглые очки", "face", 70),
        ClothingItem("32_teal_square_glasses", "Бирюзовая оправа", "face", 75),
        ClothingItem("06_denim_vest", "Джинсовый жилет", "body", 85),
    )
    private val profile = PetProfile(
        name = "Финни",
        color = PetColor.Sunny,
        clothing = ClothingState(
            ownedIds = setOf("31_round_glasses"),
        ),
    )

    override val values = sequenceOf(
        WardrobeViewState(
            loading = false,
            profile = profile,
            balanceRub = 160,
            items = items,
            mode = WardrobeMode.SHOP,
            selectedId = "06_denim_vest",
        ),
        WardrobeViewState(
            loading = false,
            profile = profile,
            items = items,
            mode = WardrobeMode.OWNED,
            selectedId = "31_round_glasses",
        ),
    )
}

package github.detrig.feature.room.presentation.furniture

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.component.FinPetBackButton
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetCoinText
import github.detrig.designsystem.component.FinPetFilterChip
import github.detrig.designsystem.component.FinPetFilterChipDefaults
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetOutlinedButton
import github.detrig.designsystem.component.FinPetStorefrontBalanceBadge
import github.detrig.designsystem.component.FinPetStorefrontCard
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.room.domain.furniture.FurnitureCatalog
import github.detrig.feature.room.domain.furniture.FurnitureSlot
import github.detrig.feature.room.domain.furniture.FurnitureStore
import github.detrig.feature.room.domain.furniture.FurnitureVariant
import github.detrig.feature.room.presentation.component.HouseBackground
import github.detrig.feature.room.presentation.component.RoomObjectLayers
import github.detrig.feature.room.presentation.component.RoomSprite
import github.detrig.feature.room.presentation.component.RoomSpriteCache
import github.detrig.feature.room.presentation.component.roomObjectAsset
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.HouseSurfaceLayout

@Composable
internal fun FurnitureStoreScreen(
    catalog: FurnitureCatalog,
    store: FurnitureStore,
    economy: EconomyApi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val viewModel: FurnitureStoreViewModel = viewModel { FurnitureStoreViewModel(catalog, store, economy) }
    val state by viewModel.state().observeAsState(FurnitureStoreViewState())
    val sprites by RoomSpriteCache.sprites(resources).collectAsState()
    LaunchedEffect(viewModel) { viewModel.perform(FurnitureStoreViewEvent.Load) }
    LaunchedEffect(state.slotId) {
        RoomSpriteCache.awaitVariants(resources, catalog.bySlot[state.slotId].orEmpty())
    }
    FurnitureStoreContent(state, catalog, sprites, viewModel::perform, onBack, modifier)
}

@Composable
private fun FurnitureStoreContent(
    state: FurnitureStoreViewState,
    catalog: FurnitureCatalog,
    sprites: Map<String, RoomSprite>,
    onEvent: (FurnitureStoreViewEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rooms = listOf("living" to "Гостиная", "bedroom" to "Спальня", "kitchen" to "Кухня")
    val slots = remember(catalog, state.roomId) { catalog.slots.filter { it.roomId == state.roomId } }
    val slot = slots.firstOrNull { it.id == state.slotId } ?: slots.first()
    val variants = catalog.bySlot[slot.id].orEmpty()
    val shown = if (state.mode == FurnitureMode.SHOP) variants else variants.filter { state.ownership.owns(it.id) }
    val selected = state.selectedVariantId?.let(catalog.byId::get)
    val equipped = state.ownership.equipped[slot.id]
    val selectedOriginal = state.originalSelected
    val canAct = (selected != null || selectedOriginal) && !state.busy
    val actionText = when {
        !canAct && state.busy -> "Подождите…"
        !canAct -> "Выберите вариант"
        selectedOriginal -> if (equipped == null) "Уже установлено" else "Установить исходный"
        selected != null && state.ownership.owns(selected.id) ->
            if (equipped == selected.id) "Уже установлено" else "Установить"
        else -> "Купить и установить · ${selected?.priceRub} ₽"
    }
    val actionEnabled = canAct && !(selectedOriginal && equipped == null) && selected?.id != equipped
    val chipState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    LaunchedEffect(state.slotId, slots) {
        val index = slots.indexOfFirst { it.id == state.slotId }
        if (index >= 0) chipState.animateScrollToItem(index)
        gridState.scrollToItem(0)
    }
    Column(modifier.fillMaxSize().background(AppTheme.colors.storefront.background).testTag("interior_store")) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            FinPetBackButton(onClick = onBack, contentDescription = "Назад к телефону")
            Text(
                "Интерьер", Modifier.weight(1f),
                style = AppTheme.typography.screenTitle,
                color = AppTheme.colors.storefront.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            FinPetStorefrontBalanceBadge(state.balanceRub.takeUnless { state.loading }, Modifier.width(100.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            listOf(FurnitureMode.SHOP to "Магазин", FurnitureMode.OWNED to "Моя мебель").forEach { (mode, label) ->
                FinPetFilterChip(
                    text = label, selected = state.mode == mode,
                    onClick = { onEvent(FurnitureStoreViewEvent.ModeSelected(mode)) },
                    modifier = Modifier.weight(1f),
                    style = FinPetFilterChipDefaults.storefrontCompactStyle(),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            rooms.forEach { (roomId, title) ->
                FinPetFilterChip(
                    text = title, selected = state.roomId == roomId,
                    onClick = { onEvent(FurnitureStoreViewEvent.RoomSelected(roomId)) },
                    modifier = Modifier.weight(1f),
                    style = FinPetFilterChipDefaults.storefrontCompactStyle(),
                )
            }
        }
        FurnitureRoomPreview(
            catalog = catalog,
            state = state,
            sprites = sprites,
            onSlotSelected = { onEvent(FurnitureStoreViewEvent.SlotSelected(it)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.xs)
                .aspectRatio(roomSegmentWidth(state.roomId) / ROOM_PREVIEW_HEIGHT),
        )
        LazyRow(
            state = chipState,
            modifier = Modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            items(slots, key = FurnitureSlot::id) { item ->
                FinPetFilterChip(
                    text = item.label,
                    selected = item.id == state.slotId,
                    onClick = { onEvent(FurnitureStoreViewEvent.SlotSelected(item.id)) },
                    modifier = Modifier.padding(start = if (item == slots.first()) AppTheme.spacing.md else 0.dp),
                    style = FinPetFilterChipDefaults.storefrontCompactStyle(),
                )
            }
        }
        LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("interior_variants"),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = AppTheme.spacing.md, end = AppTheme.spacing.md,
                    top = AppTheme.spacing.xs, bottom = AppTheme.spacing.md,
                ),
            ) {
                if (state.mode == FurnitureMode.OWNED) {
                    item(key = "original") {
                        FurnitureCard(
                            title = "Исходный",
                            price = null,
                            selected = selectedOriginal,
                            equipped = equipped == null,
                            original = true,
                            drawableId = roomObjectAsset(slot.placementId),
                            onClick = { onEvent(FurnitureStoreViewEvent.OriginalSelected) },
                        )
                    }
                }
                items(shown, key = FurnitureVariant::id) { variant ->
                    FurnitureCard(
                        title = variant.name,
                        price = if (state.ownership.owns(variant.id)) null else variant.priceRub,
                        selected = variant.id == selected?.id,
                        equipped = variant.id == equipped,
                        drawableId = variant.drawableId,
                        onClick = { onEvent(FurnitureStoreViewEvent.VariantSelected(variant.id)) },
                    )
                }
            }
        Box(
            Modifier.fillMaxWidth().background(AppTheme.colors.currencyContainer)
                .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        ) {
            FinPetButton(
                text = actionText,
                onClick = { onEvent(FurnitureStoreViewEvent.ActionPressed) },
                enabled = actionEnabled,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
        }
    }
    if (state.confirmationVisible && selected != null) {
        FinPetModalDialog(
            title = "Купить мебель?",
            onDismissRequest = { onEvent(FurnitureStoreViewEvent.PurchaseDismissed) },
            actions = {
                FinPetButton(
                    text = "Купить и установить · ${selected.priceRub} ₽",
                    onClick = { onEvent(FurnitureStoreViewEvent.PurchaseConfirmed) },
                    enabled = state.balanceRub >= selected.priceRub,
                    modifier = Modifier.fillMaxWidth(),
                    style = FinPetButtonDefaults.storefrontPrimaryStyle(),
                )
                FinPetOutlinedButton(
                    text = "Пока нет", onClick = { onEvent(FurnitureStoreViewEvent.PurchaseDismissed) },
                    modifier = Modifier.fillMaxWidth(),
                    style = FinPetButtonDefaults.storefrontOutlinedStyle(),
                )
            },
        ) {
            Text("${slot.label}: ${selected.name}", style = AppTheme.typography.bodyStrong)
            Text("Украшение комнаты · ${selected.priceRub} монет", style = AppTheme.typography.body)
            Text("Сейчас: ${state.balanceRub} · После покупки: ${state.balanceRub - selected.priceRub}",
                style = AppTheme.typography.body)
            if (state.balanceRub < selected.priceRub) {
                Text("Не хватает ${selected.priceRub - state.balanceRub} монет", style = AppTheme.typography.body)
            }
        }
    }
    state.receipt?.let { receipt ->
        FinPetModalDialog(
            title = "Чек",
            onDismissRequest = { onEvent(FurnitureStoreViewEvent.ReceiptDismissed) },
            actions = {
                FinPetButton("Готово", { onEvent(FurnitureStoreViewEvent.ReceiptDismissed) },
                    modifier = Modifier.fillMaxWidth(), style = FinPetButtonDefaults.storefrontPrimaryStyle())
            },
        ) {
            Text(receipt.itemName, style = AppTheme.typography.bodyStrong)
            Text("Украшение комнаты · ${receipt.priceRub} монет", style = AppTheme.typography.body)
            Text("Установлено в комнате", style = AppTheme.typography.body)
            Text("Осталось: ${receipt.balanceRub} монет", style = AppTheme.typography.body)
        }
    }
    state.message?.let { message ->
        FinPetModalDialog(
            title = "Интерьер", onDismissRequest = { onEvent(FurnitureStoreViewEvent.MessageDismissed) },
            actions = {
                FinPetButton("Понятно", { onEvent(FurnitureStoreViewEvent.MessageDismissed) },
                    modifier = Modifier.fillMaxWidth(), style = FinPetButtonDefaults.storefrontPrimaryStyle())
            },
        ) { Text(message, style = AppTheme.typography.body) }
    }
}

@Composable
private fun FurnitureCard(
    title: String,
    price: Long?,
    selected: Boolean,
    equipped: Boolean,
    original: Boolean = false,
    drawableId: Int,
    onClick: () -> Unit,
) {
    FinPetStorefrontCard(
        modifier = Modifier.fillMaxWidth().height(130.dp).clickable(onClick = onClick),
        containerColor = if (selected) AppTheme.colors.storefront.selectedSurface else AppTheme.colors.storefront.surface,
    ) {
        Column(Modifier.fillMaxSize().padding(AppTheme.spacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(drawableId), contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(75.dp), contentScale = ContentScale.Fit)
            Text(title, style = AppTheme.typography.caption, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            val badgeModifier = Modifier.fillMaxWidth().clip(AppTheme.shapes.storefrontControl)
                .background(if (price == null) AppTheme.colors.storefront.selectedSurface else AppTheme.colors.currencyContainer)
            if (price != null && !equipped) {
                FinPetCoinText(
                    text = "₽ $price",
                    style = AppTheme.typography.caption,
                    textAlign = TextAlign.Center,
                    modifier = badgeModifier,
                )
            } else {
                Text(
                    text = when { equipped -> "Установлено"; original -> "В наличии"; else -> "Куплено" },
                    style = AppTheme.typography.caption,
                    textAlign = TextAlign.Center,
                    modifier = badgeModifier,
                )
            }
        }
    }
}

@Composable
private fun FurnitureRoomPreview(
    catalog: FurnitureCatalog,
    state: FurnitureStoreViewState,
    sprites: Map<String, RoomSprite>,
    onSlotSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var zoomed by remember { mutableStateOf(false) }
    val equippedVariants = remember(catalog, state.ownership) {
        state.ownership.equipped.values.mapNotNull(catalog.byId::get).associateBy(FurnitureVariant::placementId)
    }
    val previewVariants = remember(catalog, state, equippedVariants, sprites) {
        val current = equippedVariants.toMutableMap()
        val selectedPlacement = catalog.slots.firstOrNull { it.id == state.slotId }?.placementId
        if (selectedPlacement != null) {
            if (state.originalSelected) current.remove(selectedPlacement)
            else state.selectedVariantId?.let(catalog.byId::get)?.takeIf { it.id in sprites }
                ?.let { current[selectedPlacement] = it }
        }
        current
    }
    Box(modifier.clip(AppTheme.shapes.card).background(AppTheme.colors.house.floor)) {
        FurnitureRoomScene(catalog, state.roomId, previewVariants, onSlotSelected, Modifier.fillMaxSize())
        FinPetOutlinedButton(
            text = "⌕", onClick = { zoomed = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(AppTheme.spacing.xs)
                .size(width = 46.dp, height = 42.dp),
            style = FinPetButtonDefaults.storefrontOutlinedStyle(),
        )
    }
    if (zoomed) {
        FinPetModalDialog(
            title = "Комната", onDismissRequest = { zoomed = false },
            actions = {
                FinPetButton("Закрыть", { zoomed = false }, modifier = Modifier.fillMaxWidth(),
                    style = FinPetButtonDefaults.storefrontPrimaryStyle())
            },
        ) {
            FurnitureRoomScene(catalog, state.roomId, previewVariants, onSlotSelected,
                Modifier.fillMaxWidth().aspectRatio(roomSegmentWidth(state.roomId) / ROOM_SCENE_HEIGHT),
                topInset = 0f)
        }
    }
}

@Composable
private fun FurnitureRoomScene(
    catalog: FurnitureCatalog,
    roomId: String,
    variants: Map<String, FurnitureVariant>,
    onSlotSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Float = ROOM_PREVIEW_TOP,
) {
    val segment = roomSegment(roomId)
    val slots = remember(catalog, roomId) { catalog.slots.filter { it.roomId == roomId } }
    val labels = remember(slots) { slots.associate { it.placementId to it.label } }
    val placements = remember(slots) {
        HouseLayout.objects.filter { it.id in labels }.map { it.copy(interactive = true) }
    }
    BoxWithConstraints(modifier.clip(AppTheme.shapes.card)) {
        val sceneWidth = maxWidth * (ROOM_SCENE_WIDTH / (segment.second - segment.first))
        val sceneHeight = maxWidth * (ROOM_SCENE_HEIGHT / (segment.second - segment.first))
        // requiredWidth overflows its parent and Compose centers that overflow.
        // Compensate on both axes so the chosen room keeps the original scene scale.
        val sceneOffset = -maxWidth * (segment.first / (segment.second - segment.first)) +
            (sceneWidth - maxWidth) / 2f
        val verticalOffset = (sceneHeight - maxHeight) / 2f -
            maxWidth * (topInset / (segment.second - segment.first))
        Box(Modifier.requiredWidth(sceneWidth).requiredHeight(sceneHeight)
            .offset(x = sceneOffset, y = verticalOffset)) {
            HouseBackground(Modifier.fillMaxSize())
            RoomObjectLayers(
                zones = emptyList(), enabled = true, buyingZoneId = null,
                onObjectClick = { placementId ->
                    catalog.slotByPlacement[placementId]?.let { onSlotSelected(it.id) }
                },
                placements = placements,
                furnitureByPlacement = variants,
                objectLabels = labels,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val ROOM_SCENE_WIDTH = HouseSurfaceLayout.SCENE_WIDTH.toFloat()
private val ROOM_SCENE_HEIGHT = HouseSurfaceLayout.SCENE_HEIGHT.toFloat()
private const val ROOM_PREVIEW_TOP = 80f
private const val ROOM_PREVIEW_HEIGHT = 505f // Keep all furniture within the visible scene.

private fun roomSegment(roomId: String): Pair<Float, Float> =
    (HouseSurfaceLayout.Room.fromId(roomId) ?: HouseSurfaceLayout.Room.KITCHEN).let {
        it.left.toFloat() to it.right.toFloat()
    }

private fun roomSegmentWidth(roomId: String): Float = roomSegment(roomId).let { it.second - it.first }

@Preview(name = "Магазин интерьера", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun FurnitureStoreContentPreview() {
    FinPetTheme {
        val resources = LocalResources.current
        val catalog = remember(resources) { FurnitureCatalog(resources) }
        FurnitureStoreContent(
            state = FurnitureStoreViewState(loading = false, balanceRub = 4_905),
            catalog = catalog, sprites = emptyMap(), onEvent = {}, onBack = {},
        )
    }
}

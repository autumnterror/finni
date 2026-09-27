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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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
import github.detrig.feature.room.domain.surface.SurfaceCatalog
import github.detrig.feature.room.domain.surface.SurfaceKind
import github.detrig.feature.room.domain.surface.SurfaceVariant
import github.detrig.feature.room.domain.surface.surfaceSlotId
import github.detrig.feature.room.presentation.component.HouseBackground
import github.detrig.feature.room.presentation.component.BathroomScene
import github.detrig.feature.room.presentation.component.bathOriginalDrawable
import github.detrig.feature.room.presentation.component.RoomObjectLayers
import github.detrig.feature.room.presentation.component.RoomSprite
import github.detrig.feature.room.presentation.component.RoomSpriteCache
import github.detrig.feature.room.presentation.component.RoomSurfaceCache
import github.detrig.feature.room.presentation.component.roomObjectAsset
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.HouseSurfaceLayout
import github.detrig.feature.room.presentation.model.HouseSurfaceTextures

@Composable
internal fun FurnitureStoreScreen(
    catalog: FurnitureCatalog,
    surfaceCatalog: SurfaceCatalog,
    store: FurnitureStore,
    economy: EconomyApi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val viewModel: FurnitureStoreViewModel = viewModel { FurnitureStoreViewModel(catalog, surfaceCatalog, store, economy) }
    val state by viewModel.state().observeAsState(FurnitureStoreViewState())
    val sprites by RoomSpriteCache.sprites(resources).collectAsState()
    val surfaceThumbnails by RoomSurfaceCache.thumbnails.collectAsState()
    val surfaceFull by RoomSurfaceCache.full.collectAsState()
    LaunchedEffect(viewModel) { viewModel.perform(FurnitureStoreViewEvent.Load) }
    LaunchedEffect(state.slotId) {
        if (state.roomId != "bathroom") {
            RoomSpriteCache.awaitVariants(resources, catalog.bySlot[state.slotId].orEmpty())
        }
    }
    val room = HouseSurfaceLayout.Room.fromId(state.roomId)
    val activeKind = state.surfaceKind
    val activeSurfaces = if (room != null && activeKind != null) {
        surfaceCatalog.bySlot[surfaceSlotId(room.id, activeKind)].orEmpty()
    } else emptyList()
    LaunchedEffect(resources, activeSurfaces) { RoomSurfaceCache.awaitThumbnails(resources, activeSurfaces) }
    val previewSurfaceIds = state.ownership.equippedSurfaces.values.toSet() + listOfNotNull(state.selectedSurfaceId)
    val missingFull = previewSurfaceIds.mapNotNull(surfaceCatalog.byId::get).filterNot { it.id in surfaceFull }
    LaunchedEffect(resources, missingFull.map { it.id }) { RoomSurfaceCache.awaitFull(resources, missingFull) }
    FurnitureStoreContent(state, catalog, surfaceCatalog, sprites, surfaceThumbnails, surfaceFull,
        viewModel::perform, onBack, modifier)
}

@Composable
private fun FurnitureStoreContent(
    state: FurnitureStoreViewState,
    catalog: FurnitureCatalog,
    surfaceCatalog: SurfaceCatalog,
    sprites: Map<String, RoomSprite>,
    surfaceThumbnails: Map<String, ImageBitmap>,
    surfaceFull: Map<String, ImageBitmap>,
    onEvent: (FurnitureStoreViewEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rooms = listOf("living" to "Гостиная", "bedroom" to "Спальня", "kitchen" to "Кухня", "bathroom" to "Ванная", "playroom" to "Игровая")
    val room = HouseSurfaceLayout.Room.fromId(state.roomId)
    val slots = remember(catalog, state.roomId) { catalog.slots.filter { it.roomId == state.roomId } }
    val slot = slots.firstOrNull { it.id == state.slotId } ?: slots.firstOrNull()
    val variants = slot?.let { catalog.bySlot[it.id] }.orEmpty()
    val shown = if (state.mode == FurnitureMode.SHOP) variants else variants.filter { state.ownership.owns(it.id) }
    val selected = state.selectedVariantId?.let(catalog.byId::get)
    val kind = state.surfaceKind
    val surfaceVariants = kind?.let { surfaceKind -> room?.let { surfaceCatalog.bySlot[surfaceSlotId(it.id, surfaceKind)] } }.orEmpty()
    val shownSurfaces = if (state.mode == FurnitureMode.SHOP) surfaceVariants
        else surfaceVariants.filter { state.ownership.ownsSurface(it.id) }
    val selectedSurface = state.selectedSurfaceId?.let(surfaceCatalog.byId::get)
    val equipped = if (kind == null) state.ownership.equipped[slot?.id]
        else room?.let { state.ownership.equippedSurfaces[surfaceSlotId(it.id, kind)] }
    val selectedOriginal = if (kind == null) state.originalSelected else state.surfaceOriginalSelected
    val selectedId = if (kind == null) selected?.id else selectedSurface?.id
    val selectedPrice = if (kind == null) selected?.priceRub else selectedSurface?.priceRub
    val selectedName = if (kind == null) selected?.name else selectedSurface?.name
    val isOwned = selectedId?.let { if (kind == null) state.ownership.owns(it) else state.ownership.ownsSurface(it) } == true
    val canAct = (selectedId != null || selectedOriginal) && !state.busy
    val actionText = when {
        !canAct && state.busy -> "Подождите…"
        !canAct -> "Выберите вариант"
        selectedOriginal -> if (equipped == null) "Уже установлено" else "Установить исходный"
        isOwned -> if (equipped == selectedId) "Уже установлено" else "Установить"
        else -> "Купить и установить · $selectedPrice ₽"
    }
    val actionEnabled = canAct && !(selectedOriginal && equipped == null) && selectedId != equipped
    val chipState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    LaunchedEffect(state.slotId, state.surfaceKind, slots) {
        val furnitureIndex = slots.indexOfFirst { it.id == state.slotId }
        val index = if (kind != null) kind.ordinal else if (furnitureIndex <= 0) 0 else furnitureIndex + 2
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
            listOf(FurnitureMode.SHOP to "Магазин", FurnitureMode.OWNED to "Мой интерьер").forEach { (mode, label) ->
                FinPetFilterChip(
                    text = label, selected = state.mode == mode,
                    onClick = { onEvent(FurnitureStoreViewEvent.ModeSelected(mode)) },
                    modifier = Modifier.weight(1f),
                    style = FinPetFilterChipDefaults.storefrontCompactStyle(),
                )
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = AppTheme.spacing.md),
        ) {
            items(rooms, key = { it.first }) { (roomId, title) ->
                FinPetFilterChip(
                    text = title, selected = state.roomId == roomId,
                    onClick = { onEvent(FurnitureStoreViewEvent.RoomSelected(roomId)) },
                    modifier = Modifier.widthIn(min = 106.dp),
                    style = FinPetFilterChipDefaults.storefrontCompactStyle(),
                )
            }
        }
        FurnitureRoomPreview(
            catalog = catalog,
            surfaceCatalog = surfaceCatalog,
            state = state,
            sprites = sprites,
            surfaceFull = surfaceFull,
            onSlotSelected = { onEvent(FurnitureStoreViewEvent.SlotSelected(it)) },
            modifier = Modifier.fillMaxWidth(if (room == HouseSurfaceLayout.Room.PLAYROOM) 0.58f else 1f)
                .align(Alignment.CenterHorizontally).padding(horizontal = AppTheme.spacing.xs)
                .aspectRatio(roomSegmentWidth(state.roomId) / ROOM_PREVIEW_HEIGHT),
        )
        LazyRow(
            state = chipState,
            modifier = Modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            items(if (state.roomId == "bathroom") emptyList() else SurfaceKind.entries, key = { "surface:${it.id}" }) { item ->
                FinPetFilterChip(
                    text = item.label,
                    selected = state.surfaceKind == item,
                    onClick = { onEvent(FurnitureStoreViewEvent.SurfaceSelected(item)) },
                    modifier = Modifier.padding(start = if (item == SurfaceKind.WALL) AppTheme.spacing.md else 0.dp),
                    style = FinPetFilterChipDefaults.storefrontCompactStyle(),
                )
            }
            items(slots, key = FurnitureSlot::id) { item ->
                FinPetFilterChip(
                    text = item.label,
                    selected = state.surfaceKind == null && item.id == state.slotId,
                    onClick = { onEvent(FurnitureStoreViewEvent.SlotSelected(item.id)) },
                    modifier = Modifier,
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
                if (kind != null) {
                    if (state.mode == FurnitureMode.OWNED) item(key = "original-surface") {
                        FurnitureCard(
                            title = "Исходный", price = null, selected = selectedOriginal,
                            equipped = equipped == null, original = true, drawableId = null,
                            onClick = { onEvent(FurnitureStoreViewEvent.OriginalSelected) },
                        )
                    }
                    items(shownSurfaces, key = SurfaceVariant::id) { variant ->
                        FurnitureCard(
                            title = variant.name,
                            price = if (state.ownership.ownsSurface(variant.id)) null else variant.priceRub,
                            selected = variant.id == selectedSurface?.id,
                            equipped = variant.id == equipped,
                            bitmap = surfaceThumbnails[variant.id],
                            onClick = { onEvent(FurnitureStoreViewEvent.SurfaceVariantSelected(variant.id)) },
                        )
                    }
                } else {
                    if (state.mode == FurnitureMode.OWNED && slot != null) item(key = "original") {
                        FurnitureCard(
                            title = "Исходный", price = null, selected = selectedOriginal,
                            equipped = equipped == null, original = true,
                            drawableId = if (state.roomId == "bathroom") bathOriginalDrawable(slot.placementId)
                                else roomObjectAsset(slot.placementId),
                            onClick = { onEvent(FurnitureStoreViewEvent.OriginalSelected) },
                        )
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
    if (state.confirmationVisible && selectedName != null && selectedPrice != null) {
        FinPetModalDialog(
            title = if (kind == null) "Купить мебель?" else "Купить ${if (kind == SurfaceKind.WALL) "обои" else "пол"}?",
            onDismissRequest = { onEvent(FurnitureStoreViewEvent.PurchaseDismissed) },
            actions = {
                FinPetButton(
                    text = "Купить и установить · $selectedPrice ₽",
                    onClick = { onEvent(FurnitureStoreViewEvent.PurchaseConfirmed) },
                    enabled = state.balanceRub >= selectedPrice,
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
            Text("${if (kind == null) slot?.label else kind.label}: $selectedName", style = AppTheme.typography.bodyStrong)
            Text("Украшение комнаты · $selectedPrice монет", style = AppTheme.typography.body)
            Text("Сейчас: ${state.balanceRub} · После покупки: ${state.balanceRub - selectedPrice}",
                style = AppTheme.typography.body)
            if (state.balanceRub < selectedPrice) {
                Text("Не хватает ${selectedPrice - state.balanceRub} монет", style = AppTheme.typography.body)
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
            Text("${receipt.categoryLabel} для комнаты · ${receipt.priceRub} монет", style = AppTheme.typography.body)
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
    drawableId: Int? = null,
    bitmap: ImageBitmap? = null,
    onClick: () -> Unit,
) {
    val captionLineHeight = with(LocalDensity.current) { AppTheme.typography.caption.lineHeight.toDp() }
    val titleHeight = captionLineHeight * 2 + AppTheme.spacing.xs
    val badgeHeight = captionLineHeight + AppTheme.spacing.xs
    FinPetStorefrontCard(
        modifier = Modifier.fillMaxWidth()
            .height(75.dp + titleHeight + badgeHeight + AppTheme.spacing.xs * 2)
            .clickable(onClick = onClick),
        containerColor = if (selected) AppTheme.colors.storefront.selectedSurface else AppTheme.colors.storefront.surface,
    ) {
        Column(Modifier.fillMaxSize().padding(AppTheme.spacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                bitmap != null -> Image(bitmap = bitmap, contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(75.dp), contentScale = ContentScale.Fit)
                drawableId != null -> Image(painter = painterResource(drawableId), contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(75.dp), contentScale = ContentScale.Fit)
                else -> Box(Modifier.fillMaxWidth().height(75.dp), contentAlignment = Alignment.Center) {
                    Text(if (original) "↺" else "…", style = AppTheme.typography.screenTitle)
                }
            }
            Box(Modifier.fillMaxWidth().height(titleHeight), contentAlignment = Alignment.Center) {
                Text(title.replace("-", "-\u200B"), style = AppTheme.typography.caption, maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            val badgeModifier = Modifier.fillMaxWidth().height(badgeHeight).clip(AppTheme.shapes.storefrontControl)
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
    surfaceCatalog: SurfaceCatalog,
    state: FurnitureStoreViewState,
    sprites: Map<String, RoomSprite>,
    surfaceFull: Map<String, ImageBitmap>,
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
            else state.selectedVariantId?.let(catalog.byId::get)?.takeIf {
                state.roomId == "bathroom" || it.id in sprites
            }
                ?.let { current[selectedPlacement] = it }
        }
        current
    }
    val previewSurfaces = remember(surfaceCatalog, state, surfaceFull) {
        val walls = mutableMapOf<HouseSurfaceLayout.Room, ImageBitmap>()
        val floors = mutableMapOf<HouseSurfaceLayout.Room, ImageBitmap>()
        state.ownership.equippedSurfaces.values.mapNotNull(surfaceCatalog.byId::get).forEach { variant ->
            surfaceFull[variant.id]?.let { bitmap ->
                HouseSurfaceLayout.Room.fromId(variant.roomId)?.let { variantRoom ->
                    if (variant.kind == SurfaceKind.WALL) walls[variantRoom] = bitmap else floors[variantRoom] = bitmap
                }
            }
        }
        val room = HouseSurfaceLayout.Room.fromId(state.roomId)
        if (room != null) {
            val selectedMap = if (state.surfaceKind == SurfaceKind.WALL) walls else floors
            if (state.surfaceOriginalSelected) selectedMap.remove(room)
            else state.selectedSurfaceId?.let(surfaceCatalog.byId::get)?.takeIf { it.roomId == room.id }
                ?.let { variant -> surfaceFull[variant.id]?.let { selectedMap[room] = it } }
        }
        HouseSurfaceTextures(walls, floors)
    }
    Box(modifier.clip(AppTheme.shapes.card).background(AppTheme.colors.house.floor)) {
        if (state.roomId == "bathroom") {
            BathroomScene(closeUp = false, equipped = previewVariants,
                modifier = Modifier.fillMaxSize(), onSlotClick = onSlotSelected)
        } else FurnitureRoomScene(catalog, state.roomId, previewVariants, previewSurfaces,
            onSlotSelected, Modifier.fillMaxSize())
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
            if (state.roomId == "bathroom") {
                BathroomScene(closeUp = false, equipped = previewVariants,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1600f / 1050f),
                    onSlotClick = onSlotSelected)
            } else FurnitureRoomScene(catalog, state.roomId, previewVariants, previewSurfaces, onSlotSelected,
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
    surfaces: HouseSurfaceTextures,
    onSlotSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Float = ROOM_PREVIEW_TOP,
) {
    val segment = roomSegment(roomId)
    val slots = remember(catalog, roomId) { catalog.slots.filter { it.roomId == roomId } }
    val labels = remember(slots) { slots.associate { it.placementId to it.label } }
    val placements = remember(slots, roomId) {
        if (roomId == HouseSurfaceLayout.Room.PLAYROOM.id) {
            HouseLayout.objects.filter { placement ->
                placement.bounds?.centerX?.times(ROOM_SCENE_WIDTH)?.let { x ->
                    x >= segment.first && x < segment.second
                } == true
            }.map { it.copy(interactive = false) }
        } else {
            HouseLayout.objects.filter { it.id in labels }.map { it.copy(interactive = true) }
        }
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
            HouseBackground(Modifier.fillMaxSize(), surfaces)
            RoomObjectLayers(
                zones = emptyList(), enabled = true, buyingZoneId = null,
                onObjectClick = { placementId ->
                    catalog.slotByPlacement[placementId]?.let { onSlotSelected(it.id) }
                },
                placements = placements,
                furnitureByPlacement = variants.filterKeys { id -> HouseLayout.objects.any { it.id == id } },
                objectLabels = labels,
                exposeInteractions = slots.isNotEmpty(),
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

private fun roomSegmentWidth(roomId: String): Float =
    if (roomId == "bathroom") 1600f * ROOM_PREVIEW_HEIGHT / 1050f
    else roomSegment(roomId).let { it.second - it.first }

@Preview(name = "Магазин интерьера", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun FurnitureStoreContentPreview() {
    FinPetTheme {
        val resources = LocalResources.current
        val catalog = remember(resources) { FurnitureCatalog(resources) }
        val surfaceCatalog = remember(resources) { SurfaceCatalog(resources) }
        FurnitureStoreContent(
            state = FurnitureStoreViewState(loading = false, balanceRub = 4_905),
            catalog = catalog, surfaceCatalog = surfaceCatalog, sprites = emptyMap(),
            surfaceThumbnails = emptyMap(), surfaceFull = emptyMap(), onEvent = {}, onBack = {},
        )
    }
}

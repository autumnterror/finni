package github.detrig.feature.pet.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetBackButton
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.pet.R
import github.detrig.feature.pet.domain.model.HamsterAppearance
import github.detrig.feature.pet.presentation.HamsterAssets
import github.detrig.feature.pet.presentation.HamsterPreview
import github.detrig.feature.pet.presentation.PetViewEvent
import github.detrig.feature.pet.presentation.PetViewState
import github.detrig.feature.pet.presentation.rememberHamsterAssets
import github.detrig.feature.pet.presentation.rememberHamsterBlink

private enum class HamsterCategory(
    val key: String,
    @StringRes val tabTitle: Int,
    @StringRes val sectionTitle: Int,
) {
    Palette("palette", R.string.hamster_category_palette, R.string.hamster_section_palette),
    Coat("coat", R.string.hamster_category_coat, R.string.hamster_section_coat),
    Fur("fur", R.string.hamster_category_fur, R.string.hamster_section_fur),
    Ears("ears", R.string.hamster_category_ears, R.string.hamster_section_ears),
    Mark("mark", R.string.hamster_category_mark, R.string.hamster_section_mark),
    Eyes("eyes", R.string.hamster_category_eyes, R.string.hamster_section_eyes),
}

private data class HamsterOption(
    val id: String,
    @StringRes val label: Int,
)

private val hamsterOptions = mapOf(
    HamsterCategory.Palette to listOf(
        HamsterOption("caramel", R.string.hamster_palette_caramel),
        HamsterOption("cream", R.string.hamster_palette_cream),
        HamsterOption("chocolate", R.string.hamster_palette_chocolate),
        HamsterOption("gray", R.string.hamster_palette_gray),
        HamsterOption("taupe", R.string.hamster_palette_taupe),
        HamsterOption("white", R.string.hamster_palette_white),
    ),
    HamsterCategory.Coat to listOf(
        HamsterOption("belly", R.string.hamster_coat_belly),
        HamsterOption("plain", R.string.hamster_coat_plain),
        HamsterOption("spots", R.string.hamster_coat_spots),
        HamsterOption("belt", R.string.hamster_coat_belt),
        HamsterOption("cap", R.string.hamster_coat_cap),
    ),
    HamsterCategory.Fur to listOf(
        HamsterOption("smooth", R.string.hamster_fur_smooth),
        HamsterOption("cheeks", R.string.hamster_fur_cheeks),
        HamsterOption("fluffy", R.string.hamster_fur_fluffy),
    ),
    HamsterCategory.Ears to listOf(
        HamsterOption("round", R.string.hamster_ears_round),
        HamsterOption("wide", R.string.hamster_ears_wide),
        HamsterOption("folded", R.string.hamster_ears_folded),
    ),
    HamsterCategory.Mark to listOf(
        HamsterOption("none", R.string.hamster_mark_none),
        HamsterOption("paw", R.string.hamster_mark_paw),
        HamsterOption("earspot", R.string.hamster_mark_earspot),
        HamsterOption("tuft", R.string.hamster_mark_tuft),
    ),
    HamsterCategory.Eyes to listOf(
        HamsterOption("round", R.string.hamster_eyes_round),
        HamsterOption("oval", R.string.hamster_eyes_oval),
    ),
)

@Composable
internal fun PetCreationScreen(
    state: PetViewState.Creating,
    onEvent: (PetViewEvent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val assets = rememberHamsterAssets()
    val blink = rememberHamsterBlink()
    var categoryName by rememberSaveable { mutableStateOf(HamsterCategory.Palette.name) }
    val category = HamsterCategory.valueOf(categoryName)
    HamsterCustomizationContent(
        appearance = state.hamsterAppearance,
        assets = assets,
        category = category,
        blink = blink,
        canFinish = assets != null,
        modifier = modifier,
        onBack = onBack,
        onCategorySelected = { categoryName = it.name },
        onAppearanceChanged = { onEvent(PetViewEvent.HamsterAppearanceChanged(it)) },
        onReset = { onEvent(PetViewEvent.HamsterAppearanceChanged(HamsterAppearance())) },
        onDone = { onEvent(PetViewEvent.CreateClicked) },
    )
}

@Composable
private fun HamsterCustomizationContent(
    appearance: HamsterAppearance,
    assets: HamsterAssets?,
    category: HamsterCategory,
    blink: Boolean,
    canFinish: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onCategorySelected: (HamsterCategory) -> Unit,
    onAppearanceChanged: (HamsterAppearance) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.storefront.background)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                ),
            )
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header(onBack = onBack, onReset = onReset)
        Spacer(Modifier.height(AppTheme.spacing.sm))
        HamsterStage(assets = assets, appearance = appearance, blink = blink)
        Spacer(Modifier.height(AppTheme.spacing.md))
        CategoryTabs(selected = category, onSelected = onCategorySelected)
        Spacer(Modifier.height(AppTheme.spacing.md))
        Text(
            text = androidx.compose.ui.res.stringResource(category.sectionTitle),
            modifier = Modifier.fillMaxWidth(),
            style = AppTheme.typography.sectionTitle,
            color = AppTheme.colors.storefront.onSurface,
        )
        Spacer(Modifier.height(AppTheme.spacing.sm))
        OptionGrid(
            category = category,
            appearance = appearance,
            assets = assets,
            onSelected = { option ->
                onAppearanceChanged(appearance.with(category, option.id))
            },
        )
        Spacer(Modifier.height(AppTheme.spacing.lg))
        FinPetButton(
            text = androidx.compose.ui.res.stringResource(R.string.hamster_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.preferredTouchTarget),
            enabled = canFinish,
            style = FinPetButtonDefaults.storefrontPrimaryStyle(),
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit, onReset: () -> Unit) {
    val resetDescription = androidx.compose.ui.res.stringResource(R.string.hamster_reset)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FinPetBackButton(
            onClick = onBack,
            contentDescription = androidx.compose.ui.res.stringResource(R.string.hamster_back),
        )
        Text(
            text = androidx.compose.ui.res.stringResource(R.string.hamster_title),
            style = AppTheme.typography.screenTitle,
            color = AppTheme.colors.storefront.onSurface,
            textAlign = TextAlign.Center,
        )
        FinPetButton(
            onClick = onReset,
            modifier = Modifier
                .size(AppTheme.sizes.preferredTouchTarget)
                .semantics {
                    contentDescription = resetDescription
                },
            style = FinPetButtonDefaults.storefrontOutlinedStyle().copy(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(AppTheme.spacing.none),
            ),
        ) {
            ResetIcon()
        }
    }
}

@Composable
private fun ResetIcon() {
    val outline = AppTheme.colors.storefront.outline
    val borderWidth = AppTheme.sizes.borderStrong
    Canvas(Modifier.size(AppTheme.sizes.iconLarge)) {
        val stroke = borderWidth.toPx()
        drawArc(
            color = outline,
            startAngle = 42f,
            sweepAngle = 285f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )
        drawLine(
            color = outline,
            start = androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .28f),
            end = androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .52f),
            strokeWidth = stroke,
        )
        drawLine(
            color = outline,
            start = androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .52f),
            end = androidx.compose.ui.geometry.Offset(size.width * .43f, size.height * .52f),
            strokeWidth = stroke,
        )
    }
}

@Composable
private fun HamsterStage(
    assets: HamsterAssets?,
    appearance: HamsterAppearance,
    blink: Boolean,
) {
    val background = AppTheme.colors.storefront.background
    val stripe = AppTheme.colors.currencyContainer.copy(alpha = .28f)
    val shadow = AppTheme.colors.currencyAccent.copy(alpha = .18f)
    Box(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(background)
            val stripeWidth = size.width / 8f
            repeat(8) { index ->
                if (index % 2 == 1) {
                    drawRect(
                        color = stripe,
                        topLeft = androidx.compose.ui.geometry.Offset(index * stripeWidth, 0f),
                        size = androidx.compose.ui.geometry.Size(stripeWidth, size.height),
                    )
                }
            }
            drawOval(
                color = shadow,
                topLeft = androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .83f),
                size = androidx.compose.ui.geometry.Size(size.width * .6f, size.height * .1f),
            )
        }
        if (assets != null) {
            HamsterPreview(
                assets = assets,
                appearance = appearance,
                modifier = Modifier.size(260.dp).padding(AppTheme.spacing.xs),
                blink = blink,
            )
        } else {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.hamster_loading),
                style = AppTheme.typography.body,
                color = AppTheme.colors.storefront.onSurface,
            )
        }
    }
}

@Composable
private fun CategoryTabs(
    selected: HamsterCategory,
    onSelected: (HamsterCategory) -> Unit,
) {
    Column(
        modifier = Modifier.semantics { selectableGroup() },
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        HamsterCategory.entries.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                row.forEach { category ->
                    val style = if (category == selected) {
                        FinPetButtonDefaults.storefrontPrimaryStyle()
                    } else {
                        FinPetButtonDefaults.storefrontOutlinedStyle()
                    }
                    FinPetButton(
                        text = androidx.compose.ui.res.stringResource(category.tabTitle),
                        onClick = { onSelected(category) },
                        modifier = Modifier.weight(1f),
                        style = style,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionGrid(
    category: HamsterCategory,
    appearance: HamsterAppearance,
    assets: HamsterAssets?,
    onSelected: (HamsterOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        hamsterOptions.getValue(category).chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                row.forEach { option ->
                    HamsterOptionCard(
                        category = category,
                        option = option,
                        selected = appearance.value(category) == option.id,
                        assets = assets,
                        onClick = { onSelected(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HamsterOptionCard(
    category: HamsterCategory,
    option: HamsterOption,
    selected: Boolean,
    assets: HamsterAssets?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = androidx.compose.ui.res.stringResource(option.label)
    FinPetCard(
        modifier = modifier
            .heightIn(min = 112.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label },
        shape = AppTheme.shapes.storefrontControl,
        containerColor = if (selected) {
            AppTheme.colors.storefront.selectedSurface
        } else {
            AppTheme.colors.storefront.surface
        },
        contentColor = AppTheme.colors.storefront.onSurface,
        borderColor = if (selected) {
            AppTheme.colors.actionPrimary
        } else {
            AppTheme.colors.storefront.outline
        },
        borderWidth = if (selected) AppTheme.sizes.borderStrong else AppTheme.sizes.borderThin,
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                if (category == HamsterCategory.Palette) {
                    val color = assets?.palettes?.get(option.id)?.get("base")
                        ?: AppTheme.colors.petColorSunny
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(color)
                            .then(
                                if (option.id == "white") {
                                    Modifier.border(
                                        AppTheme.sizes.borderThin,
                                        AppTheme.colors.storefront.outline,
                                        androidx.compose.foundation.shape.CircleShape,
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                    )
                } else {
                    val thumbnail = assets?.thumbnails?.get("${category.key}_${option.id}")
                    if (thumbnail != null) {
                        androidx.compose.foundation.Image(
                            bitmap = thumbnail,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.High,
                        )
                    } else {
                        Spacer(Modifier.size(64.dp))
                    }
                }
                Text(
                    text = label,
                    style = AppTheme.typography.label,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
            if (selected) {
                Text(
                    text = "✓",
                    modifier = Modifier.align(Alignment.TopEnd).padding(AppTheme.spacing.xs),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.actionPrimary,
                )
            }
        }
    }
}

private fun HamsterAppearance.value(category: HamsterCategory): String = when (category) {
    HamsterCategory.Palette -> palette
    HamsterCategory.Coat -> coat
    HamsterCategory.Fur -> fur
    HamsterCategory.Ears -> ears
    HamsterCategory.Mark -> mark
    HamsterCategory.Eyes -> eyes
}

private fun HamsterAppearance.with(category: HamsterCategory, value: String): HamsterAppearance = when (category) {
    HamsterCategory.Palette -> copy(palette = value)
    HamsterCategory.Coat -> copy(coat = value)
    HamsterCategory.Fur -> copy(fur = value)
    HamsterCategory.Ears -> copy(ears = value)
    HamsterCategory.Mark -> copy(mark = value)
    HamsterCategory.Eyes -> copy(eyes = value)
}

@Preview(name = "Hamster customization", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HamsterCustomizationPreview() {
    FinPetTheme {
        HamsterCustomizationContent(
            appearance = HamsterAppearance(),
            assets = null,
            category = HamsterCategory.Palette,
            blink = false,
            canFinish = false,
            onBack = {},
            onCategorySelected = {},
            onAppearanceChanged = {},
            onReset = {},
            onDone = {},
        )
    }
}

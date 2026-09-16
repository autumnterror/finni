package github.detrig.feature.pet.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.pet.R
import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.PetNameRules
import github.detrig.feature.pet.domain.model.PetNameValidationError
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.model.PetSpecies
import github.detrig.feature.pet.presentation.PetScene
import github.detrig.feature.pet.presentation.PetViewEvent
import github.detrig.feature.pet.presentation.PetViewState
import github.detrig.feature.pet.presentation.tint
import github.detrig.feature.pet.presentation.title
import kotlinx.coroutines.delay

@Composable
internal fun PetCreationScreen(
    state: PetViewState.Creating,
    onEvent: (PetViewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = imeBottom > 0
    val scrollState = rememberScrollState()
    var nameFieldBottom by remember { mutableFloatStateOf(0f) }
    val keyboardGapPx = with(density) { AppTheme.spacing.sm.toPx() }

    LaunchedEffect(imeBottom) {
        if (imeBottom > 0) {
            delay(100)
            val overlap = nameFieldBottom - (view.height - imeBottom) + keyboardGapPx
            if (overlap > 0f) scrollState.animateScrollBy(overlap)
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(AppTheme.colors.surfaceBase),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                    ),
                )
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .widthIn(max = AppTheme.sizes.contentMaxWidth)
                .padding(horizontal = AppTheme.spacing.xl, vertical = AppTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.pet_creation_title),
                style = AppTheme.typography.screenTitle,
                color = AppTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AppTheme.spacing.xs))
            Text(
                text = stringResource(R.string.pet_creation_subtitle),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AppTheme.spacing.lg))
            PetScene(
                profile = PetProfile(
                    name = state.name.takeIf { PetNameRules.validate(it) == null } ?: stringResource(R.string.pet_preview_name),
                    species = state.species,
                    color = state.color,
                ),
                modifier = Modifier.fillMaxWidth().heightIn(
                    min = AppTheme.sizes.illustrationMinHeight,
                    max = AppTheme.sizes.illustrationMaxHeight,
                ),
            )
            Spacer(Modifier.height(AppTheme.spacing.xl))
            SectionTitle(stringResource(R.string.pet_species_title))
            SpeciesOptions(
                selected = state.species,
                onSelected = { onEvent(PetViewEvent.SpeciesSelected(it)) },
            )
            Spacer(Modifier.height(AppTheme.spacing.lg))
            SectionTitle(stringResource(R.string.pet_color_title))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PetColor.entries.forEach { color ->
                    ColorOption(
                        color = color,
                        selected = color == state.color,
                        onClick = { onEvent(PetViewEvent.ColorSelected(color)) },
                    )
                }
            }
            Spacer(Modifier.height(AppTheme.spacing.lg))
            SectionTitle(stringResource(R.string.pet_name_title))
            OutlinedTextField(
                value = state.name,
                onValueChange = { onEvent(PetViewEvent.NameChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { nameFieldBottom = it.boundsInWindow().bottom },
                label = { Text(stringResource(R.string.pet_name_label)) },
                placeholder = { Text(stringResource(R.string.pet_name_hint)) },
                supportingText = if (isKeyboardVisible) {
                    null
                } else {
                    {
                        val error = state.nameError
                        Text(
                            text = error?.message() ?: stringResource(
                                R.string.pet_name_counter,
                                state.name.codePointCount(0, state.name.length),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = if (error == null) TextAlign.End else TextAlign.Start,
                        )
                    }
                },
                isError = state.nameError != null,
                singleLine = true,
                shape = AppTheme.shapes.button,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.actionPrimary,
                    focusedLabelColor = AppTheme.colors.actionPrimary,
                    errorBorderColor = AppTheme.colors.statusCritical.accent,
                    errorLabelColor = AppTheme.colors.statusCritical.accent,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (state.canCreate) onEvent(PetViewEvent.CreateClicked) },
                ),
            )
            if (isKeyboardVisible) {
                Spacer(
                    Modifier.height(
                        AppTheme.sizes.preferredTouchTarget + AppTheme.spacing.sm,
                    ),
                )
            } else {
                Spacer(Modifier.height(AppTheme.spacing.lg))
                FinPetButton(
                    text = stringResource(R.string.pet_create_button),
                    onClick = { onEvent(PetViewEvent.CreateClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canCreate,
                )
                Spacer(Modifier.height(AppTheme.spacing.lg))
            }
        }
    }
}

@Composable
private fun SpeciesOptions(
    selected: PetSpecies,
    onSelected: (PetSpecies) -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < AppTheme.sizes.preferredTouchTarget * 6 || fontScale > 1.1f
        val columns = if (compact) 2 else PetSpecies.entries.size
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            PetSpecies.entries.chunked(columns).forEach { speciesRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    speciesRow.forEach { species ->
                        SpeciesOption(
                            species = species,
                            selected = species == selected,
                            onClick = { onSelected(species) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - speciesRow.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(bottom = AppTheme.spacing.sm),
        style = AppTheme.typography.sectionTitle,
        color = AppTheme.colors.textPrimary,
    )
}

@Composable
private fun SpeciesOption(
    species: PetSpecies,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = species.title()
    Surface(
        modifier = modifier
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = title },
        shape = AppTheme.shapes.card,
        color = if (selected) AppTheme.colors.actionSecondary else AppTheme.colors.surfaceElevated,
        contentColor = AppTheme.colors.textPrimary,
        border = BorderStroke(
            width = if (selected) AppTheme.sizes.borderStrong else AppTheme.sizes.borderThin,
            color = if (selected) AppTheme.colors.actionPrimary else AppTheme.colors.borderDefault,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PetScene(
                profile = PetProfile(stringResource(R.string.pet_preview_name), species, PetColor.Sunny),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                animateIdle = false,
            )
            Text(
                text = title,
                style = AppTheme.typography.label,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ColorOption(
    color: PetColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val title = color.title()
    val swatchColor = color.tint()
    Surface(
        modifier = Modifier
            .size(AppTheme.sizes.preferredTouchTarget)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = title },
        shape = androidx.compose.foundation.shape.CircleShape,
        color = AppTheme.colors.surfaceElevated,
        border = BorderStroke(
            width = if (selected) AppTheme.sizes.borderStrong else AppTheme.sizes.borderThin,
            color = if (selected) AppTheme.colors.actionPrimary else AppTheme.colors.borderDefault,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(AppTheme.sizes.iconLarge)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(swatchColor)
                    .then(
                        if (selected) Modifier.border(
                            AppTheme.sizes.borderThin,
                            AppTheme.colors.onActionSecondary,
                            androidx.compose.foundation.shape.CircleShape,
                        ) else Modifier,
                    ),
            )
        }
    }
}

@Composable
private fun PetNameValidationError.message(): String = stringResource(
    when (this) {
        PetNameValidationError.Empty -> R.string.pet_name_error_empty
        PetNameValidationError.TooLong -> R.string.pet_name_error_too_long
        PetNameValidationError.InvalidCharacters -> R.string.pet_name_error_characters
    },
)

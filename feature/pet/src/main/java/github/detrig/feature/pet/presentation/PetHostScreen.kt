package github.detrig.feature.pet.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.core.mvvm.command.CommandsQueueEffect
import github.detrig.core.mvvm.command.ImmutableCommandsQueue
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.pet.PetFeature
import github.detrig.feature.pet.R
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.presentation.component.PetCreationScreen

@Composable
internal fun PetHostScreen(
    modifier: Modifier = Modifier,
    content: @Composable (PetProfile, () -> Unit, () -> Unit, Boolean) -> Unit,
) {
    val viewModel: PetViewModel = viewModel { PetFeature.component().getPetViewModel() }
    val state by viewModel.state().observeAsState(PetViewState.Loading)
    var greetingVisible by rememberSaveable { mutableStateOf(false) }
    val commands = remember(viewModel) { ImmutableCommandsQueue(viewModel.commands<PetCommand>()) }
    CommandsQueueEffect(commands) { command ->
        when (command) {
            PetCommand.ShowGreeting -> greetingVisible = true
        }
    }
    LaunchedEffect(viewModel) { viewModel.perform(PetViewEvent.Load) }

    when (val current = state) {
        PetViewState.Loading -> Box(
            modifier = modifier.fillMaxSize().background(AppTheme.colors.surfaceBase),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = AppTheme.colors.actionPrimary)
        }
        is PetViewState.Creating -> PetCreationScreen(
            state = current,
            onEvent = viewModel::perform,
            modifier = modifier,
            onBack = { viewModel.perform(PetViewEvent.CancelCustomization) },
        )
        is PetViewState.Ready -> {
            content(
                current.profile,
                { viewModel.perform(PetViewEvent.PetClicked) },
                { viewModel.perform(PetViewEvent.CustomizeClicked) },
                greetingVisible,
            )
            if (greetingVisible) {
                FinPetDialogueDialog(
                    speakerName = current.profile.name,
                    cards = listOf(
                        stringResource(R.string.pet_welcome_first, current.profile.name),
                        stringResource(R.string.pet_welcome_second),
                    ),
                    portrait = { portraitModifier ->
                        PetPortrait(current.profile, portraitModifier)
                    },
                    onFinished = { greetingVisible = false },
                )
            }
        }
    }
}

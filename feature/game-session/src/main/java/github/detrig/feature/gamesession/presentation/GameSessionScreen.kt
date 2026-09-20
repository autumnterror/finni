package github.detrig.feature.gamesession.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.feature.gamesession.GameSessionFeature
import github.detrig.feature.gamesession.presentation.component.GameSessionHome

@Composable
internal fun GameSessionScreen() {
    val component = GameSessionFeature.component()
    component.petApi.RequirePet(modifier = Modifier) {
        petProfile,
        onPetClick,
        onCustomizeClick,
        isPetDialogueVisible,
        ->
        GameSessionHome { modifier ->
            component.roomApi.Content(
                modifier = modifier,
                petName = petProfile.name,
                canShowDialogs = !isPetDialogueVisible,
                petContent = { petModifier ->
                    component.petApi.Content(
                        profile = petProfile,
                        modifier = petModifier,
                        onClick = onPetClick,
                    )
                },
                petPortrait = { portraitModifier ->
                    component.petApi.Portrait(petProfile, portraitModifier)
                },
                onMirrorClick = onCustomizeClick,
                onPhoneClick = { component.phoneApi.open() },
            )
        }
    }
}

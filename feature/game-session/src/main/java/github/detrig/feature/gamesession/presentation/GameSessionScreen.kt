package github.detrig.feature.gamesession.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.feature.gamesession.GameSessionFeature
import github.detrig.feature.gamesession.presentation.component.GameSessionHome

@Composable
internal fun GameSessionScreen() {
    val component = GameSessionFeature.component()
    component.petApi.RequirePet(modifier = Modifier) { petProfile, onPetClick, onCustomizeClick ->
        GameSessionHome { modifier ->
            component.roomApi.Content(
                modifier = modifier,
                onMirrorClick = onCustomizeClick,
                onPhoneClick = { component.phoneApi.open() },
                onFoodClick = { component.fridgeApi.open() },
                onFeedingClick = { component.fridgeApi.openFeeding() },
                tableFoodContent = { tableModifier -> component.fridgeApi.TableContent(tableModifier) },
                petContent = { petModifier ->
                    component.petApi.Content(
                        profile = petProfile,
                        modifier = petModifier,
                        onClick = onPetClick,
                    )
                },
            )
        }
    }
}

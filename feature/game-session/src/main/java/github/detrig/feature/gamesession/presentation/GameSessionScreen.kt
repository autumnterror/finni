package github.detrig.feature.gamesession.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.feature.gamesession.GameSessionFeature
import github.detrig.feature.gamesession.presentation.component.GameSessionHome

@Composable
internal fun GameSessionScreen() {
    val component = GameSessionFeature.component()
    component.phoneApi.RoomNotifications { phoneState, dismissFirstPrompt ->
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
                    onMirrorClick = onCustomizeClick,
                    onPhoneClick = { component.phoneApi.open() },
                    phoneUnreadCount = phoneState.unreadCount,
                    phoneNotificationPrompt = phoneState.firstPrompt,
                    onPhonePromptOpen = { component.phoneApi.openMessages() },
                    onPhonePromptDismiss = dismissFirstPrompt,
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
                    petPortrait = { portraitModifier ->
                        component.petApi.Portrait(petProfile, portraitModifier)
                    },
                )
            }
        }
    }
}

package github.detrig.minigames.fishing.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.minigames.fishing.domain.FishingProgress
import github.detrig.minigames.fishing.domain.FishingSession
import github.detrig.minigames.fishing.domain.FishingPreferences
import github.detrig.minigames.fishing.api.FishingEnvironment

internal enum class FishingPage { GAME, RECORDS, RESULTS, PRACTICE_INFO }
internal enum class FishingError { LOAD, CHECKPOINT, CATCH, FINISH, EFFECT, SETTINGS }
internal enum class FishingConfirmation { EXIT, RESTART, DISCARD_UNSAVED }
internal data class FishingViewState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val environment: FishingEnvironment? = null,
    val progress: FishingProgress? = null,
    val page: FishingPage = FishingPage.GAME,
    val hud: FishingSession? = null,
    val confirmation: FishingConfirmation? = null,
    val error: FishingError? = null,
    val tutorialJustFinished: Boolean = false,
) : CoreViewState

internal sealed interface FishingViewEvent : CoreViewEvent {
    data object Load : FishingViewEvent
    data object Start : FishingViewEvent
    data object Continue : FishingViewEvent
    data object Tutorial : FishingViewEvent
    data object SkipTutorial : FishingViewEvent
    data object Press : FishingViewEvent
    data object Release : FishingViewEvent
    data object CancelPress : FishingViewEvent
    data object Toggle : FishingViewEvent
    data object Recast : FishingViewEvent
    data object Pause : FishingViewEvent
    data object Back : FishingViewEvent
    data object Records : FishingViewEvent
    data object FinishPractice : FishingViewEvent
    data object DismissMigration : FishingViewEvent
    data object Restart : FishingViewEvent
    data object Exit : FishingViewEvent
    data object Confirm : FishingViewEvent
    data object Dismiss : FishingViewEvent
    data object Retry : FishingViewEvent
    data class Preferences(val value: FishingPreferences) : FishingViewEvent
}

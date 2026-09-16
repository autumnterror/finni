package github.detrig.feature.gamesession.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.gamesession.domain.interactor.GameSessionState
import github.detrig.feature.gamesession.domain.interactor.ObserveGameStateInteractor
import github.detrig.feature.gamesession.navigation.GameSessionRouter
import github.detrig.feature.gamesession.presentation.animation.toPetAnimationState
import github.detrig.feature.gamesession.presentation.formatter.toAllowanceCountdownText
import github.detrig.feature.gamesession.presentation.formatter.toRubText
import kotlinx.coroutines.Job

internal class GameSessionViewModel(
    private val observeGameStateInteractor: ObserveGameStateInteractor,
    private val router: GameSessionRouter,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : CoreViewModel<GameSessionViewState, GameSessionViewEvent>(
    initialState = GameSessionViewState.Loading,
) {

    private var observationJob: Job? = null

    override fun perform(viewEvent: GameSessionViewEvent) {
        when (viewEvent) {
            GameSessionViewEvent.Load -> loadSession()
            GameSessionViewEvent.ShopClicked -> showFutureFeature("Магазин")
            GameSessionViewEvent.WorkClicked -> showFutureFeature("Работа")
            GameSessionViewEvent.TasksClicked -> showFutureFeature("Задания")
            GameSessionViewEvent.BankClicked -> showFutureFeature("Банк")
            GameSessionViewEvent.CareClicked -> showFutureFeature("Уход")
            GameSessionViewEvent.HomeClicked -> showFutureFeature("Дом")
        }
    }

    private fun loadSession() {
        if (observationJob?.isActive == true) return
        observationJob = launchCoroutine(
            handleAction = localErrorHandler(),
        ) {
            updateState(GameSessionViewState.Loading)
            observeGameStateInteractor().collect { gameState ->
                updateState(gameState.toContentState())
            }
        }
    }

    private fun showFutureFeature(featureName: String) {
        router.showMessage("$featureName будет отдельной фичей")
    }

    private fun localErrorHandler(): ExceptionConsumer {
        return ExceptionConsumer { exception ->
            updateState(GameSessionViewState.Error)
            router.showErrorMessage(exception.message ?: "Не удалось обновить игровую сессию")
            true
        }
    }

    private fun GameSessionState.toContentState(): GameSessionViewState.Content {
        return GameSessionViewState.Content(
            balanceText = economy.availableRub.toRubText(),
            nextAllowanceText = "+${economy.periodicIncome.amountRub.toRubText()} ${
                economy.periodicIncome.nextAtMillis.toAllowanceCountdownText(currentTimeMillis())
            }",
            levelText = "Уровень ${game.playerLevel}",
            hunger = game.pet.hunger,
            thirst = game.pet.thirst,
            happiness = game.pet.happiness,
            health = game.pet.health,
            petAnimationState = game.pet.toPetAnimationState(),
        )
    }
}

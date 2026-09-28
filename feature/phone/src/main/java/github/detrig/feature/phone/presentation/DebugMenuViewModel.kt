package github.detrig.feature.phone.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.CoreViewState
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.pet.domain.model.GrowthStage
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.phone.navigation.PhoneRouter
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect

internal data class DebugMenuViewState(
    val balanceRub: Long = 0,
    val growthStage: GrowthStage = GrowthStage.BABY,
    val growthStageFromLevel: GrowthStage = GrowthStage.BABY,
    val isGrowthStageOverridden: Boolean = false,
    val isChangingGrowthStage: Boolean = false,
    val dirtStage: Int = 0,
    val isChanging: Boolean = false,
    val isChangingDirtStage: Boolean = false,
    val isEndingWeek: Boolean = false,
    val isResettingProgress: Boolean = false,
    val pendingReset: DebugProgressResetMode? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
) : CoreViewState

internal sealed interface DebugMenuViewEvent : CoreViewEvent {
    data object Load : DebugMenuViewEvent
    data class ChangeBalance(val deltaRub: Long) : DebugMenuViewEvent
    data object ResetBalance : DebugMenuViewEvent
    data class ChangeGrowthStage(val delta: Int) : DebugMenuViewEvent
    data object UseLevelGrowthStage : DebugMenuViewEvent
    data class ChangeDirtStage(val delta: Int) : DebugMenuViewEvent
    data object EndWeek : DebugMenuViewEvent
    data class RequestProgressReset(val mode: DebugProgressResetMode) : DebugMenuViewEvent
    data object CancelProgressReset : DebugMenuViewEvent
    data object ConfirmProgressReset : DebugMenuViewEvent
}

internal enum class DebugProgressResetMode(val skipOnboarding: Boolean) {
    BEFORE_ONBOARDING(skipOnboarding = false),
    AFTER_ONBOARDING(skipOnboarding = true),
}

internal class DebugMenuViewModel(
    private val economyApi: EconomyApi,
    private val weekApi: WeekApi,
    private val gameStateApi: GameStateApi,
    private val resetDemoProgress: suspend (skipOnboarding: Boolean) -> Unit,
    private val petApi: PetApi,
    private val router: PhoneRouter,
) : CoreViewModel<DebugMenuViewState, DebugMenuViewEvent>(DebugMenuViewState()) {
    private var observationJob: Job? = null
    private var dirtObservationJob: Job? = null
    private var changeJob: Job? = null
    private var growthObservationJob: Job? = null
    private var growthChangeJob: Job? = null
    private var dirtChangeJob: Job? = null
    private var endWeekJob: Job? = null
    private var resetProgressJob: Job? = null

    override fun perform(viewEvent: DebugMenuViewEvent) {
        when (viewEvent) {
            DebugMenuViewEvent.Load -> {
                observeBalance()
                observeGrowthStage()
                observeDirtStage()
            }
            is DebugMenuViewEvent.ChangeBalance -> changeBalance(viewEvent.deltaRub)
            is DebugMenuViewEvent.ChangeGrowthStage -> changeGrowthStage(viewEvent.delta)
            DebugMenuViewEvent.UseLevelGrowthStage -> useLevelGrowthStage()
            is DebugMenuViewEvent.ChangeDirtStage -> changeDirtStage(viewEvent.delta)
            DebugMenuViewEvent.ResetBalance -> {
                val balance = stateData.balanceRub
                if (balance > 0) changeBalance(-balance)
            }
            DebugMenuViewEvent.EndWeek -> endWeek()
            is DebugMenuViewEvent.RequestProgressReset ->
                updateState { copy(pendingReset = viewEvent.mode, errorMessage = null) }
            DebugMenuViewEvent.CancelProgressReset ->
                if (!stateData.isResettingProgress) updateState { copy(pendingReset = null) }
            DebugMenuViewEvent.ConfirmProgressReset ->
                stateData.pendingReset?.let(::resetProgress)
        }
    }

    private fun observeBalance() {
        if (observationJob?.isActive == true) return
        observationJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(errorMessage = "Не удалось загрузить баланс") }
                true
            },
        ) {
            economyApi.initialize()
            economyApi.observeState().collect { economy ->
                updateState { copy(balanceRub = economy.availableRub) }
            }
        }
    }

    private fun observeGrowthStage() {
        if (growthObservationJob?.isActive == true) return
        growthObservationJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(errorMessage = "Не удалось загрузить стадию роста") }
                true
            },
        ) {
            petApi.observeGrowthState().collect { growth ->
                updateState {
                    copy(
                        growthStage = growth.stage,
                        growthStageFromLevel = growth.stageFromLevel,
                        isGrowthStageOverridden = growth.isDebugOverride,
                    )
                }
            }
        }
    }

    private fun observeDirtStage() {
        if (dirtObservationJob?.isActive == true) return
        dirtObservationJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(errorMessage = "Не удалось загрузить загрязнение") }
                true
            },
        ) {
            val gameState = gameStateApi.initialize()
            updateState { copy(dirtStage = gameState.pet.dirtStage) }
            gameStateApi.observeState().collect { state ->
                state?.let { updateState { copy(dirtStage = it.pet.dirtStage) } }
            }
        }
    }

    private fun changeGrowthStage(delta: Int) {
        if (delta !in listOf(-1, 1) || growthChangeJob?.isActive == true || stateData.isResettingProgress) return
        updateState { copy(isChangingGrowthStage = true, errorMessage = null) }
        growthChangeJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(isChangingGrowthStage = false, errorMessage = "Не удалось изменить стадию роста") }
                growthChangeJob = null
                true
            },
        ) {
            val growth = petApi.adjustGrowthStageForDebug(delta)
            updateState {
                copy(
                    growthStage = growth.stage,
                    isGrowthStageOverridden = growth.isDebugOverride,
                    isChangingGrowthStage = false,
                )
            }
            growthChangeJob = null
        }
    }

    private fun useLevelGrowthStage() {
        if (growthChangeJob?.isActive == true || stateData.isResettingProgress) return
        updateState { copy(isChangingGrowthStage = true, errorMessage = null) }
        growthChangeJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(isChangingGrowthStage = false, errorMessage = "Не удалось вернуть рост по уровню") }
                growthChangeJob = null
                true
            },
        ) {
            val growth = petApi.useLevelGrowthStageForDebug()
            updateState {
                copy(
                    growthStage = growth.stage,
                    growthStageFromLevel = growth.stageFromLevel,
                    isGrowthStageOverridden = false,
                    isChangingGrowthStage = false,
                )
            }
            growthChangeJob = null
        }
    }

    private fun changeDirtStage(delta: Int) {
        if (delta !in listOf(-1, 1) || dirtChangeJob?.isActive == true || stateData.isResettingProgress) return
        updateState { copy(isChangingDirtStage = true, errorMessage = null) }
        dirtChangeJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(isChangingDirtStage = false, errorMessage = "Не удалось изменить загрязнение") }
                dirtChangeJob = null
                true
            },
        ) {
            val stage = gameStateApi.adjustPetDirtStageForDebug(delta)
            updateState { copy(dirtStage = stage, isChangingDirtStage = false) }
            dirtChangeJob = null
        }
    }

    private fun changeBalance(deltaRub: Long) {
        if (deltaRub == 0L || changeJob?.isActive == true || stateData.isResettingProgress) return
        updateState { copy(isChanging = true, errorMessage = null) }
        changeJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(isChanging = false, errorMessage = "Не удалось изменить баланс") }
                changeJob = null
                true
            },
        ) {
            val operationId = "debug-menu:${UUID.randomUUID()}"
            val context = OperationContext(reasonId = "debug-menu")
            val result = if (deltaRub > 0) {
                economyApi.credit(operationId, deltaRub, context)
            } else {
                economyApi.debit(operationId, -deltaRub, context)
            }
            updateState {
                copy(
                    balanceRub = result.state.availableRub,
                    isChanging = false,
                    errorMessage = if (result is FinancialOperationResult.Rejected) {
                        "Баланс не может стать отрицательным"
                    } else {
                        null
                    },
                )
            }
            changeJob = null
        }
    }

    private fun endWeek() {
        if (endWeekJob?.isActive == true || stateData.isResettingProgress) return
        updateState { copy(isEndingWeek = true, errorMessage = null, statusMessage = null) }
        endWeekJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState {
                    copy(
                        isEndingWeek = false,
                        errorMessage = "Не удалось завершить неделю",
                        statusMessage = null,
                    )
                }
                endWeekJob = null
                true
            },
        ) {
            weekApi.skipToSundayForDebug()
            updateState {
                copy(
                    isEndingWeek = false,
                    statusMessage = "Воскресенье. Вернитесь в комнату и нажмите на кровать, чтобы увидеть итоги недели.",
                )
            }
            endWeekJob = null
        }
    }

    private fun resetProgress(mode: DebugProgressResetMode) {
        if (resetProgressJob?.isActive == true || changeJob?.isActive == true ||
            endWeekJob?.isActive == true || growthChangeJob?.isActive == true ||
            dirtChangeJob?.isActive == true) return
        updateState {
            copy(isResettingProgress = true, pendingReset = null, errorMessage = null, statusMessage = null)
        }
        resetProgressJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState {
                    copy(
                        isResettingProgress = false,
                        errorMessage = "Не удалось сбросить прогресс",
                    )
                }
                resetProgressJob = null
                true
            },
        ) {
            resetDemoProgress(mode.skipOnboarding)
            updateState { copy(isResettingProgress = false) }
            petApi.resetProfile()
            router.close()
            resetProgressJob = null
        }
    }
}

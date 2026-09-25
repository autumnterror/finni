package github.detrig.feature.phone.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.CoreViewState
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.week.api.WeekApi
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect

internal data class DebugMenuViewState(
    val balanceRub: Long = 0,
    val isChanging: Boolean = false,
    val isEndingWeek: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
) : CoreViewState

internal sealed interface DebugMenuViewEvent : CoreViewEvent {
    data object Load : DebugMenuViewEvent
    data class ChangeBalance(val deltaRub: Long) : DebugMenuViewEvent
    data object ResetBalance : DebugMenuViewEvent
    data object EndWeek : DebugMenuViewEvent
}

internal class DebugMenuViewModel(
    private val economyApi: EconomyApi,
    private val weekApi: WeekApi,
) : CoreViewModel<DebugMenuViewState, DebugMenuViewEvent>(DebugMenuViewState()) {
    private var observationJob: Job? = null
    private var changeJob: Job? = null
    private var endWeekJob: Job? = null

    override fun perform(viewEvent: DebugMenuViewEvent) {
        when (viewEvent) {
            DebugMenuViewEvent.Load -> observeBalance()
            is DebugMenuViewEvent.ChangeBalance -> changeBalance(viewEvent.deltaRub)
            DebugMenuViewEvent.ResetBalance -> {
                val balance = stateData.balanceRub
                if (balance > 0) changeBalance(-balance)
            }
            DebugMenuViewEvent.EndWeek -> endWeek()
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

    private fun changeBalance(deltaRub: Long) {
        if (deltaRub == 0L || changeJob?.isActive == true) return
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
        if (endWeekJob?.isActive == true) return
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
            val current = weekApi.initialize()
            val targetSunday = current.weekNumber * 7
            var day = current
            while (day.absoluteDay < targetSunday) {
                day = weekApi.endDay(day.absoluteDay).state
            }
            updateState {
                copy(
                    isEndingWeek = false,
                    statusMessage = "Воскресенье. Вернитесь в комнату и нажмите на кровать, чтобы увидеть итоги недели.",
                )
            }
            endWeekJob = null
        }
    }
}

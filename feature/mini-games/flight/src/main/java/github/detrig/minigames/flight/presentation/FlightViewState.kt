package github.detrig.minigames.flight.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.minigames.flight.api.FlightEnvironment
import github.detrig.minigames.flight.api.FlightFeedbackSettings
import github.detrig.minigames.flight.domain.FlightProgress
import github.detrig.minigames.flight.domain.FlightSession

internal enum class FlightPage { LOADING, ERROR, LOCKED, COUNTDOWN, PLAYING, SAVING, RESULTS, RECORDS }

internal data class FlightViewState(
    val page: FlightPage = FlightPage.LOADING,
    val environment: FlightEnvironment? = null,
    val progress: FlightProgress? = null,
    val settings: FlightFeedbackSettings = FlightFeedbackSettings(),
    val countdown: Int = 0,
    val countdownGeneration: Int = 0,
    val foreground: Boolean = true,
    val score: Int = 0,
    val effectPending: Boolean = false,
) : CoreViewState

/** Дробь шага нужна только отрисовке, в сохранение и физику не попадает. */
internal data class FlightRenderFrame(val session: FlightSession? = null, val fraction: Float = 0f)
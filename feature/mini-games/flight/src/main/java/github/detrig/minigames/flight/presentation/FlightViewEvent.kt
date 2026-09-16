package github.detrig.minigames.flight.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.core.mvvm.command.ViewCommand
import github.detrig.minigames.flight.api.FlightFeedbackSettings

internal sealed interface FlightViewEvent : CoreViewEvent {
    data object Load : FlightViewEvent
    data object Start : FlightViewEvent
    data object Flap : FlightViewEvent
    data class Frame(val sessionId: String, val nanos: Long) : FlightViewEvent
    data class CountdownTick(val sessionId: String, val generation: Int) : FlightViewEvent
    data class Foreground(val active: Boolean) : FlightViewEvent
    data object Back : FlightViewEvent
    data object Exit : FlightViewEvent
    data object Records : FlightViewEvent
    data object Retry : FlightViewEvent
    data object RetryEffect : FlightViewEvent
    data class Settings(val value: FlightFeedbackSettings) : FlightViewEvent
}
internal enum class FlightCue : ViewCommand { GATE, LAND }
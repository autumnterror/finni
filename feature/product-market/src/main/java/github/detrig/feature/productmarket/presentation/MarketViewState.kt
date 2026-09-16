package github.detrig.feature.productmarket.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.productmarket.domain.MarketPhase
import github.detrig.feature.productmarket.domain.MarketTrip

internal enum class MarketError { LOAD, SAVE, BALANCE }
internal data class MarketViewState(
    val trip: MarketTrip? = null,
    val balanceRub: Int? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val cartOpen: Boolean = false,
    val exitConfirmationOpen: Boolean = false,
    val foreground: Boolean = false,
    val error: MarketError? = null,
) : CoreViewState {
    val canAdvance: Boolean get() = foreground && !loading && !busy && !cartOpen && !exitConfirmationOpen &&
        error == null && balanceRub != null && trip?.phase in setOf(MarketPhase.WALKING, MarketPhase.ARRIVED)
}

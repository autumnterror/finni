package github.detrig.feature.productmarket.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.products.ProductId

internal sealed interface MarketViewEvent : CoreViewEvent {
    data object Load : MarketViewEvent
    data object Retry : MarketViewEvent
    data class Frame(val seconds: Double) : MarketViewEvent
    data class Foreground(val active: Boolean) : MarketViewEvent
    data class Viewport(val width: Double) : MarketViewEvent
    data class Pick(val instanceId: String) : MarketViewEvent
    data class Remove(val productId: ProductId) : MarketViewEvent
    data object OpenCart : MarketViewEvent
    data object CloseCart : MarketViewEvent
    data object AnotherPass : MarketViewEvent
    data object Finish : MarketViewEvent
    data object NewTrip : MarketViewEvent
    data object Back : MarketViewEvent
    data object CancelExit : MarketViewEvent
    data object ConfirmExit : MarketViewEvent
    data object LeaveAfterError : MarketViewEvent
}

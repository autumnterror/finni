package github.detrig.feature.fridge.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.products.ProductId

internal sealed interface FridgeViewEvent : CoreViewEvent {
    data object Load : FridgeViewEvent
    data object Back : FridgeViewEvent
    data class ProductClicked(val productId: ProductId) : FridgeViewEvent
    data class FlightAnimationFinished(val productId: ProductId) : FridgeViewEvent
    data object FirstRunContinue : FridgeViewEvent
}

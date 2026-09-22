package github.detrig.feature.fridge.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.products.ProductId

internal sealed interface FeedingViewEvent : CoreViewEvent {
    data object Load : FeedingViewEvent
    data object Back : FeedingViewEvent
    data object PreviousPage : FeedingViewEvent
    data object NextPage : FeedingViewEvent
    data class FoodDroppedIntoMouth(val productId: ProductId) : FeedingViewEvent
}

package github.detrig.feature.fridge.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.products.ProductId

internal const val FEEDING_PAGE_SIZE = 3

internal data class FeedingViewState(
    val foodStacks: List<FeedingFoodStack> = emptyList(),
    val page: Int = 0,
    val hunger: Int = 0,
    val activePortion: FeedingFoodPortion? = null,
    val pendingPortions: List<FeedingFoodPortion> = emptyList(),
    val animation: FeedingAnimation = FeedingAnimation.Idle,
    val loading: Boolean = true,
    val message: String? = null,
) : CoreViewState

/** A visible stack preserves the table order while exposing one portion at a time. */
internal data class FeedingFoodStack(
    val productId: ProductId,
    val portionIds: List<String>,
) {
    val quantity: Int get() = portionIds.size
}

internal data class FeedingFoodPortion(
    val id: String,
    val productId: ProductId,
)

internal enum class FeedingAnimation {
    Idle,
    MouthOpen,
    ChewA,
    ChewB,
}

internal fun List<FeedingFoodStack>.lastFeedingPageIndex(): Int =
    ((size - 1).coerceAtLeast(0)) / FEEDING_PAGE_SIZE

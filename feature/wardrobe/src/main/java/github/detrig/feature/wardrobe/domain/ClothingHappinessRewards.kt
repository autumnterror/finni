package github.detrig.feature.wardrobe.domain

import github.detrig.feature.pet.api.ClothingItem

/** A one-time joy bonus for a new look, independent of its price. */
internal object ClothingHappinessRewards {
    fun points(item: ClothingItem): Int = when (item.slot) {
        "face" -> 12
        "neck" -> 15
        "head" -> 18
        "body" -> 20
        "back" -> 25
        else -> 12
    }
}

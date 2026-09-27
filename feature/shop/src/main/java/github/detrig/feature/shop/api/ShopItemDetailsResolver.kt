package github.detrig.feature.shop.api

import github.detrig.products.SellableItem
import github.detrig.products.FoodItem

enum class ShopItemDetailIcon { NONE, SATIETY, HAPPINESS }

data class ShopItemDetail(
    val text: String,
    val icon: ShopItemDetailIcon = ShopItemDetailIcon.NONE,
)

fun interface ShopItemDetailsResolver {
    fun details(item: SellableItem): List<ShopItemDetail>

    companion object {
        val Empty = ShopItemDetailsResolver { emptyList() }
    }
}

fun foodEffectDetails(item: SellableItem): List<ShopItemDetail> {
    val effects = (item as? FoodItem)?.effects ?: return emptyList()
    return buildList {
        if (effects.satietyPercent > 0) {
            add(ShopItemDetail("+${effects.satietyPercent}%", ShopItemDetailIcon.SATIETY))
        }
        if (effects.happinessPoints > 0) {
            add(ShopItemDetail("+${effects.happinessPoints}", ShopItemDetailIcon.HAPPINESS))
        }
    }
}

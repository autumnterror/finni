package github.detrig.feature.shop.api

import github.detrig.products.SellableItem

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

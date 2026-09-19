package github.detrig.feature.shop.api

import androidx.annotation.DrawableRes
import github.detrig.products.ProductImageKey

sealed interface ShopArtwork {
    data class Resource(
        @param:DrawableRes val drawableRes: Int,
    ) : ShopArtwork

    data class AtlasRegion(
        @param:DrawableRes val drawableRes: Int,
        val leftPx: Int,
        val topPx: Int,
        val widthPx: Int,
        val heightPx: Int,
    ) : ShopArtwork {
        init {
            require(leftPx >= 0 && topPx >= 0) { "Atlas offset must be non-negative" }
            require(widthPx > 0 && heightPx > 0) { "Atlas region must be positive" }
        }
    }
}

fun interface ShopArtworkResolver {
    fun resolve(imageKey: ProductImageKey): ShopArtwork?

    companion object {
        val Empty = ShopArtworkResolver { null }
    }
}

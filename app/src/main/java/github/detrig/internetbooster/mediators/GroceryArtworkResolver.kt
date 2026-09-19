package github.detrig.internetbooster.mediators

import androidx.annotation.DrawableRes
import github.detrig.feature.shop.api.ShopArtwork
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.products.GroceryCatalog
import github.detrig.products.ProductImageKey

/** Resolves the provided grocery reference sheet without coupling generic shop UI to its assets. */
internal class GroceryArtworkResolver(
    @DrawableRes private val atlasDrawableRes: Int,
) : ShopArtworkResolver {
    private val regions: Map<ProductImageKey, ShopArtwork.AtlasRegion> =
        GroceryCatalog().storefront.items.mapIndexed { index, item ->
            item.imageKey to regionFor(index)
        }.toMap()

    override fun resolve(imageKey: ProductImageKey): ShopArtwork? = regions[imageKey]

    private fun regionFor(catalogIndex: Int): ShopArtwork.AtlasRegion {
        val sourceColumn: Int
        val sourceTop: Int
        when {
            catalogIndex < FIRST_PRODUCT_ROW_SIZE -> {
                sourceColumn = catalogIndex
                sourceTop = FIRST_PRODUCT_ROW_TOP
            }
            catalogIndex < PRODUCT_COUNT -> {
                sourceColumn = catalogIndex - FIRST_PRODUCT_ROW_SIZE
                sourceTop = SECOND_PRODUCT_ROW_TOP
            }
            catalogIndex < PRODUCT_COUNT + MEAL_COUNT -> {
                sourceColumn = catalogIndex - PRODUCT_COUNT
                sourceTop = MEAL_ROW_TOP
            }
            else -> {
                val drinkIndex = catalogIndex - PRODUCT_COUNT - MEAL_COUNT
                sourceColumn = DRINK_SOURCE_COLUMNS[drinkIndex]
                sourceTop = DRINK_ROW_TOP
            }
        }
        return ShopArtwork.AtlasRegion(
            drawableRes = atlasDrawableRes,
            leftPx = CARD_LEFT + sourceColumn * CARD_STEP + CROP_HORIZONTAL_INSET,
            topPx = sourceTop + CROP_TOP_INSET,
            widthPx = CROP_WIDTH,
            heightPx = CROP_HEIGHT,
        )
    }

    private companion object {
        const val PRODUCT_COUNT = 20
        const val MEAL_COUNT = 10
        const val FIRST_PRODUCT_ROW_SIZE = 10

        const val CARD_LEFT = 40
        const val CARD_STEP = 160
        const val FIRST_PRODUCT_ROW_TOP = 147
        const val SECOND_PRODUCT_ROW_TOP = 327
        const val MEAL_ROW_TOP = 568
        const val DRINK_ROW_TOP = 809

        const val CROP_HORIZONTAL_INSET = 8
        const val CROP_TOP_INSET = 5
        const val CROP_WIDTH = 134
        const val CROP_HEIGHT = 124

        // The source reference contains water in column 1; the game has no thirst mechanic.
        val DRINK_SOURCE_COLUMNS = listOf(0, 2, 3, 4, 5, 6, 7, 8, 9)
    }
}

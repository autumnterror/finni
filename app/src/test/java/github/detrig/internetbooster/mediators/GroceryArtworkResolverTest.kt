package github.detrig.internetbooster.mediators

import github.detrig.feature.shop.api.ShopArtwork
import github.detrig.products.GroceryCatalog
import github.detrig.products.ProductImageKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroceryArtworkResolverTest {
    private val resolver = GroceryArtworkResolver(atlasDrawableRes = 7)

    @Test
    fun everyGroceryItemHasAValidAtlasRegion() {
        val artworks = GroceryCatalog().storefront.items.map { item ->
            resolver.resolve(item.imageKey) as ShopArtwork.AtlasRegion
        }

        assertEquals(39, artworks.size)
        assertTrue(artworks.all { it.drawableRes == 7 })
        assertTrue(artworks.all { it.leftPx >= 0 && it.topPx >= 0 })
        assertTrue(artworks.all { it.widthPx > 0 && it.heightPx > 0 })
        assertEquals(39, artworks.map { it.leftPx to it.topPx }.distinct().size)
    }

    @Test
    fun unknownImageKeyIsNotResolved() {
        assertNull(resolver.resolve(ProductImageKey("store.grocery.image.unknown")))
    }
}

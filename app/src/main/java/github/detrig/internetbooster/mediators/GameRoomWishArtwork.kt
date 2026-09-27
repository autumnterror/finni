package github.detrig.internetbooster.mediators

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.room.api.RoomWishArtwork
import github.detrig.feature.room.domain.model.RoomImpulseWish
import github.detrig.feature.shop.api.ShopArtwork
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.products.GroceryCatalog

internal class GameRoomWishArtwork(
    private val groceryArtwork: ShopArtworkResolver,
    private val petApi: PetApi,
) : RoomWishArtwork {
    private val groceries = GroceryCatalog().storefront.items.associateBy { it.id.value }

    @Composable
    override fun Content(wish: RoomImpulseWish, modifier: Modifier) {
        when (wish.kind) {
            RoomImpulseWish.Kind.CLOTHING -> wish.productId?.let {
                petApi.ClothingThumbnail(it, modifier)
            }
            RoomImpulseWish.Kind.GROCERY -> {
                val item = groceries[wish.productId] ?: return
                val artwork = groceryArtwork.resolve(item.imageKey) ?: return
                val painter = when (artwork) {
                    is ShopArtwork.Resource -> painterResource(artwork.drawableRes)
                    is ShopArtwork.AtlasRegion -> {
                        val resources = LocalResources.current
                        val atlas = remember(resources, artwork.drawableRes) {
                            ImageBitmap.imageResource(resources, artwork.drawableRes)
                        }
                        remember(atlas, artwork) {
                            BitmapPainter(
                                atlas,
                                srcOffset = IntOffset(artwork.leftPx, artwork.topPx),
                                srcSize = IntSize(artwork.widthPx, artwork.heightPx),
                            )
                        }
                    }
                }
                Image(
                    painter = painter,
                    contentDescription = wish.productTitle,
                    modifier = modifier,
                    contentScale = ContentScale.Fit,
                )
            }
            RoomImpulseWish.Kind.FREE -> Unit
        }
    }
}

package github.detrig.feature.shop.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

fun interface ShopPetPortrait {
    @Composable
    fun Content(modifier: Modifier)

    companion object {
        val Empty = ShopPetPortrait { }
    }
}

package github.detrig.feature.shop

import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.core.audio.GameAudio
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.feature.shop.api.ShopHost
import github.detrig.feature.shop.api.ShopItemDetailsResolver
import github.detrig.feature.shop.api.ShopPetPortrait
import github.detrig.feature.shop.domain.ShopCatalogRegistry

interface ShopDependencies {
    fun host(): ShopHost
    fun catalogRegistry(): ShopCatalogRegistry
    fun artworkResolver(): ShopArtworkResolver
    fun itemDetailsResolver(): ShopItemDetailsResolver
    fun petPortrait(): ShopPetPortrait
    fun globalNavigator(): GlobalNavigator
    fun gameAudio(): GameAudio
}

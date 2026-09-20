package github.detrig.feature.phone

import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.shop.api.ShopApi

interface PhoneDependencies {
    fun globalNavigator(): GlobalNavigator
    fun roomApi(): RoomApi
    fun petApi(): PetApi
    fun shopApi(): ShopApi
}

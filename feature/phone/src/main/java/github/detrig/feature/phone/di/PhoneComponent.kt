package github.detrig.feature.phone.di

import github.detrig.feature.phone.api.PhoneApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.shop.api.ShopApi
import github.detrig.feature.phone.navigation.PhoneRouter
import github.detrig.feature.phone.presentation.DebugMenuViewModel
import github.detrig.feature.phone.presentation.MessagesViewModel
import github.detrig.feature.phone.presentation.RoomNotificationsViewModel
import github.detrig.core.presentation.message.GlobalMessageController

internal interface PhoneComponent {
    val api: PhoneApi
    val router: PhoneRouter
    val roomApi: RoomApi
    val petApi: PetApi
    val shopApi: ShopApi
    val globalMessageController: GlobalMessageController
    fun debugMenuViewModel(): DebugMenuViewModel
    fun messagesViewModel(): MessagesViewModel
    fun roomNotificationsViewModel(): RoomNotificationsViewModel
}

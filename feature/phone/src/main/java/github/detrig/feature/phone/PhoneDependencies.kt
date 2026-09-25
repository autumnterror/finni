package github.detrig.feature.phone

import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.shop.api.ShopApi
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.phone.api.PhoneMessagesStorage
import kotlinx.coroutines.CoroutineScope
import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.feature.gamestate.api.ProgressionApi

interface PhoneDependencies {
    fun globalNavigator(): GlobalNavigator
    fun applicationScope(): CoroutineScope
    fun roomApi(): RoomApi
    fun petApi(): PetApi
    fun shopApi(): ShopApi
    fun economyApi(): EconomyApi
    fun weekApi(): WeekApi
    fun learningApi(): LearningApi
    fun progressionApi(): ProgressionApi
    fun messagesStorage(): PhoneMessagesStorage
    fun dailySecurityEventProbability(): Double
    fun minimumHelpBalanceRub(): Long
    fun globalMessageController(): GlobalMessageController
}

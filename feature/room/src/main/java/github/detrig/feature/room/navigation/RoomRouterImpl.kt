package github.detrig.feature.room.navigation

import android.content.res.Resources
import github.detrig.feature.room.R

import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.feature.room.api.RoomGameLauncher

internal class RoomRouterImpl(
    private val gameLauncher: RoomGameLauncher,
    private val messageController: GlobalMessageController,
    private val resources: Resources,
    private val marketLauncher: github.detrig.feature.room.api.RoomMarketLauncher,
) : RoomRouter {
    override fun openMarket() = marketLauncher.openMarket()
    override fun openGame(gameId: String) = gameLauncher.openGame(gameId)
    override fun showLevelRequired(level: Int) =
        messageController.showMessage(resources.getString(R.string.room_level_required, level))
    override fun showNotEnoughMoney(missingRub: Int) =
        messageController.showMessage(resources.getString(R.string.room_not_enough_money_advice))
    override fun showBought() =
        messageController.showSuccessMessage(resources.getString(R.string.room_bought))
    override fun showBuyError() =
        messageController.showErrorMessage(resources.getString(R.string.room_buy_error))
    override fun showSavingsGoalError() =
        messageController.showErrorMessage(resources.getString(R.string.room_savings_goal_error))
    override fun showSleepError() =
        messageController.showErrorMessage(resources.getString(R.string.room_sleep_error))
    override fun showPlanNotReady() =
        messageController.showMessage(resources.getString(R.string.plan_not_ready))
    override fun showPlanSaveError() =
        messageController.showErrorMessage(resources.getString(R.string.plan_save_error))
    override fun showEntryComingSoon() =
        messageController.showMessage(resources.getString(R.string.room_entry_coming_soon))
}

package github.detrig.feature.room.navigation

internal interface RoomRouter {
    fun openMarket()
    fun openWardrobe()
    fun openGame(gameId: String)
    fun showLevelRequired(level: Int)
    fun showNotEnoughMoney(missingRub: Int)
    fun showBought()
    fun showBuyError()
    fun showSavingsGoalError()
    fun showSleepError()
    fun showPlanNotReady()
    fun showPlanSaveError()
    fun showEntryComingSoon()
}

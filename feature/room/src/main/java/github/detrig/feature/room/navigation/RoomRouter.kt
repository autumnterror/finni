package github.detrig.feature.room.navigation

internal interface RoomRouter {
    fun openMarket()
    fun openGame(gameId: String)
    fun showLevelRequired(level: Int)
    fun showNotEnoughMoney(missingRub: Int)
    fun showBought()
    fun showBuyError()
    fun showSleepError()
    fun showPlanNotReady()
    fun showPlanSaveError()
    fun showEntryComingSoon()
}

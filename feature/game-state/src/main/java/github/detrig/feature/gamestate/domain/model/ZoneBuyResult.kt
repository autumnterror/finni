package github.detrig.feature.gamestate.domain.model

sealed interface ZoneBuyResult {
    data object Bought : ZoneBuyResult
    data object AlreadyOwned : ZoneBuyResult
    data class NotEnoughMoney(val missingRub: Int) : ZoneBuyResult
    data class LevelTooLow(val requiredLevel: Int) : ZoneBuyResult
}

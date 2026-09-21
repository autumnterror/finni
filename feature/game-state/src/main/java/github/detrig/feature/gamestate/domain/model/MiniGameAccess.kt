package github.detrig.feature.gamestate.domain.model

/** Общий свободный доступ к играм; покупки остаются отдельным фактом. */
object MiniGameAccess {
    private val initiallyOpenGameIds = setOf("ball", "flight")

    fun isInitiallyOpen(gameId: String): Boolean = gameId in initiallyOpenGameIds

    fun isOpen(gameId: String, ownedZoneIds: Set<String>): Boolean =
        isInitiallyOpen(gameId) || gameId in ownedZoneIds
}

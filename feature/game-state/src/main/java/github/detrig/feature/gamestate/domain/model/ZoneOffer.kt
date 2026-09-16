package github.detrig.feature.gamestate.domain.model

/** Условия покупки из каталога фичи. */
data class ZoneOffer(
    val zoneId: String,
    val priceRub: Int,
    val requiredLevel: Int,
) {
    init {
        require(zoneId.isNotBlank())
        require(priceRub >= 0)
        require(requiredLevel >= 1)
    }
}

package github.detrig.feature.gamestate.domain.model

data class HungerAlertState(
    val hunger: Int,
    val hungerAlertEpisode: Long,
    val hungerAlertDeliveredEpisode: Long,
) {
    val pendingEpisode: Long?
        get() = hungerAlertEpisode.takeIf {
            hunger == 0 && it > hungerAlertDeliveredEpisode
        }
}

package github.detrig.feature.room.domain.model

internal data class RoomProgress(
    val balanceRub: Int,
    val playerLevel: Int,
    val ownedZoneIds: Set<String>,
    val nextAllowanceAmountRub: Int,
    val nextAllowanceAtMillis: Long,
    val petHunger: Int = 0,
    val petHappiness: Int = 0,
)

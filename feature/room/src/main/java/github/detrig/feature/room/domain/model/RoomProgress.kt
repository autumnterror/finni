package github.detrig.feature.room.domain.model

import github.detrig.feature.planning.domain.WeeklyPlanProgress

internal data class RoomProgress(
    val balanceRub: Int,
    val savingsRub: Long = 0,
    val playerLevel: Int,
    val ownedZoneIds: Set<String>,
    val absoluteDay: Long,
    val weekNumber: Long,
    val dayOfWeek: Int,
    val daysUntilAllowance: Int,
    val planProgress: WeeklyPlanProgress? = null,
    val requiresPlan: Boolean = false,
    val petHunger: Int = 0,
    val petHappiness: Int = 0,
)

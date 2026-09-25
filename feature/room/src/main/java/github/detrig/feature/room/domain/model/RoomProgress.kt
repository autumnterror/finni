package github.detrig.feature.room.domain.model

import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.gamestate.domain.progression.PetGrowthStage

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
    val debtRub: Long = 0,
    val totalXp: Int = 0,
    val currentLevelXp: Int = 0,
    val nextLevelXp: Int? = 100,
    val experienceProgress: Float = 0f,
    val petGrowthStage: PetGrowthStage = PetGrowthStage.BABY,
) {
    val xpUntilNextLevel: Int?
        get() = nextLevelXp?.let { (it - currentLevelXp).coerceAtLeast(0) }
}

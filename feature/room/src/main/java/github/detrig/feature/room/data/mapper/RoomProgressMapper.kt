package github.detrig.feature.room.data.mapper

import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.economy.domain.EconomyState
import github.detrig.feature.week.domain.WeekState
import github.detrig.feature.planning.domain.WeeklyPlanProgress

internal fun GameState.toRoomProgress(
    economy: EconomyState,
    week: WeekState,
    planProgress: WeeklyPlanProgress?,
) = RoomProgress(
    balanceRub = Math.toIntExact(economy.availableRub),
    savingsRub = economy.savingsRub,
    playerLevel = playerLevel,
    ownedZoneIds = ownedZoneIds,
    absoluteDay = week.absoluteDay,
    weekNumber = week.weekNumber,
    dayOfWeek = week.dayOfWeek,
    daysUntilAllowance = week.daysUntilAllowance,
    planProgress = planProgress,
    requiresPlan = planProgress == null,
    petHunger = pet.hunger,
    petHappiness = pet.happiness,
)

package github.detrig.feature.room.data.mapper

import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.economy.domain.EconomyState
import github.detrig.feature.week.domain.WeekState

internal fun GameState.toRoomProgress(economy: EconomyState, week: WeekState) = RoomProgress(
    balanceRub = Math.toIntExact(economy.availableRub),
    playerLevel = playerLevel,
    ownedZoneIds = ownedZoneIds,
    absoluteDay = week.absoluteDay,
    weekNumber = week.weekNumber,
    dayOfWeek = week.dayOfWeek,
    daysUntilAllowance = week.daysUntilAllowance,
    petHunger = pet.hunger,
    petHappiness = pet.happiness,
)

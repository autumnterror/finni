package github.detrig.feature.room.data.mapper

import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.economy.domain.EconomyState

internal fun GameState.toRoomProgress(economy: EconomyState) = RoomProgress(
    balanceRub = Math.toIntExact(economy.availableRub),
    playerLevel = playerLevel,
    ownedZoneIds = ownedZoneIds,
    nextAllowanceAmountRub = Math.toIntExact(economy.periodicIncome.amountRub),
    nextAllowanceAtMillis = economy.periodicIncome.nextAtMillis,
    petHunger = pet.hunger,
    petHappiness = pet.happiness,
)

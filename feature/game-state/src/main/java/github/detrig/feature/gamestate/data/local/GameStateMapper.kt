package github.detrig.feature.gamestate.data.local

import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.gamestate.domain.PetState

internal fun GameStateWithZones.toDomain(): GameState = state.toDomain().copy(
    ownedZoneIds = zones.map { it.zoneId }.toSet(),
)

internal fun GameStateEntity.toDomain(): GameState = GameState(
    pet = PetState(
        hunger = hunger,
        thirst = thirst,
        happiness = happiness,
        health = health,
    ),
    playerLevel = playerLevel,
)

internal fun GameState.toEntity(): GameStateEntity = GameStateEntity(
    id = GameStateEntity.CURRENT_STATE_ID,
    hunger = pet.hunger,
    thirst = pet.thirst,
    happiness = pet.happiness,
    health = pet.health,
    playerLevel = playerLevel,
)

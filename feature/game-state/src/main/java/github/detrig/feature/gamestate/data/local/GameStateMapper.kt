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
    totalXp = totalXp,
)

internal fun GameState.toEntity(nowMillis: Long = 0): GameStateEntity = GameStateEntity(
    id = GameStateEntity.CURRENT_STATE_ID,
    hunger = pet.hunger,
    thirst = pet.thirst,
    happiness = pet.happiness,
    health = pet.health,
    playerLevel = playerLevel,
    totalXp = totalXp,
    hungerCheckpointMillis = nowMillis,
    happinessCheckpointMillis = nowMillis,
    hungerAlertEpisode = if (pet.hunger == 0) 1 else 0,
)

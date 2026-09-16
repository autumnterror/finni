package github.detrig.feature.room.domain.repository

import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.RoomZoneDefinition
import kotlinx.coroutines.flow.Flow

internal interface RoomRepository {
    fun zones(): List<RoomZoneDefinition>
    suspend fun initialize()
    fun observeProgress(): Flow<RoomProgress>
    suspend fun buyZone(zone: RoomZoneDefinition): ZoneBuyResult
}

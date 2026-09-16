package github.detrig.feature.room.data.repository

import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.ZoneOffer
import github.detrig.feature.room.data.catalog.RoomZoneCatalog
import github.detrig.feature.room.data.mapper.toRoomProgress
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.RoomZoneDefinition
import github.detrig.feature.room.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import github.detrig.feature.economy.api.EconomyApi

internal class RoomRepositoryImpl(
    private val catalog: RoomZoneCatalog,
    private val gameStateApi: GameStateApi,
    private val economyApi: EconomyApi,
) : RoomRepository {
    override fun zones(): List<RoomZoneDefinition> = catalog.zones

    override suspend fun initialize() {
        gameStateApi.initialize()
        economyApi.initialize()
    }

    override fun observeProgress(): Flow<RoomProgress> = combine(
        gameStateApi.observeState().filterNotNull(),
        economyApi.observeState(),
    ) { game, economy -> game.toRoomProgress(economy) }.distinctUntilChanged()

    override suspend fun buyZone(zone: RoomZoneDefinition): ZoneBuyResult = gameStateApi.buyZone(
        ZoneOffer(zone.id, zone.priceRub, zone.requiredLevel),
    )
}

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
import kotlinx.coroutines.flow.flatMapLatest
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.domain.PlanPercentages
import github.detrig.feature.planning.domain.PlanAssessment
import github.detrig.feature.planning.domain.SavePlanResult
import github.detrig.feature.planning.domain.PaymentClassification
import github.detrig.feature.planning.domain.PlanActualOperation
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.economy.domain.ParentHelpState
import kotlinx.coroutines.flow.first

internal class RoomRepositoryImpl(
    private val catalog: RoomZoneCatalog,
    private val gameStateApi: GameStateApi,
    private val economyApi: EconomyApi,
    private val weekApi: WeekApi,
    private val planningApi: PlanningApi,
) : RoomRepository {
    override fun zones(): List<RoomZoneDefinition> = catalog.zones

    override suspend fun initialize() {
        gameStateApi.initialize()
        economyApi.initialize()
        weekApi.initialize()
    }

    override fun observeProgress(): Flow<RoomProgress> = combine(
        gameStateApi.observeState().filterNotNull(),
        economyApi.observeState(),
        weekApi.observeState(),
    ) { game, economy, week -> Triple(game, economy, week) }.flatMapLatest { (game, economy, week) ->
        planningApi.observePlanProgress(week.weekNumber).map { plan ->
            game.toRoomProgress(economy, week, plan)
        }
    }.distinctUntilChanged()

    override suspend fun buyZone(zone: RoomZoneDefinition): ZoneBuyResult {
        val result = gameStateApi.buyZone(ZoneOffer(zone.id, zone.priceRub, zone.requiredLevel))
        if (result == ZoneBuyResult.Bought) {
            val week = weekApi.observeState().first()
            if (planningApi.getPlanProgress(week.weekNumber) != null) {
                planningApi.recordActual(
                    PlanActualOperation.Payment(
                        operationId = "room-zone:${zone.id}",
                        weekNumber = week.weekNumber,
                        amountRub = zone.priceRub.toLong(),
                        classification = PaymentClassification.OPTIONAL,
                    ),
                )
            }
        }
        return result
    }

    override suspend fun endDay(expectedAbsoluteDay: Long): EndDayResult = weekApi.endDay(expectedAbsoluteDay)

    override fun assessPlan(percentages: PlanPercentages): PlanAssessment = planningApi.assessPlan(percentages)

    override suspend fun savePlan(
        weekNumber: Long,
        availableRub: Long,
        percentages: PlanPercentages,
    ): SavePlanResult = planningApi.savePlan(weekNumber, availableRub, percentages)

    override fun parentHelpOffers(): List<ParentHelpOffer> = economyApi.parentHelpOffers()

    override suspend fun parentHelp(): ParentHelpState? = economyApi.getParentHelp()

    override suspend fun requestParentHelp(offerId: String): ParentHelpRequestResult {
        val week = weekApi.observeState().first()
        return economyApi.requestParentHelp(
            operationId = "parent-help:${week.weekNumber}:$offerId",
            offerId = offerId,
        )
    }

    override suspend fun endWeekEarlyWithParentHelp(
        expectedAbsoluteDay: Long,
        minimumProductPriceRub: Long,
    ) = weekApi.endWeekEarlyWithParentHelp(expectedAbsoluteDay, minimumProductPriceRub)
}

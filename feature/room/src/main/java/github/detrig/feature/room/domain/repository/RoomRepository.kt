package github.detrig.feature.room.domain.repository

import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.RoomZoneDefinition
import kotlinx.coroutines.flow.Flow
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.planning.domain.PlanPercentages
import github.detrig.feature.planning.domain.SavePlanResult
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.economy.domain.ParentHelpState

internal interface RoomRepository {
    fun zones(): List<RoomZoneDefinition>
    suspend fun initialize()
    fun observeProgress(): Flow<RoomProgress>
    suspend fun buyZone(zone: RoomZoneDefinition): ZoneBuyResult
    suspend fun endDay(expectedAbsoluteDay: Long): EndDayResult
    suspend fun savePlan(weekNumber: Long, availableRub: Long, percentages: PlanPercentages): SavePlanResult
    fun parentHelpOffers(): List<ParentHelpOffer>
    suspend fun parentHelp(): ParentHelpState?
    suspend fun requestParentHelp(offerId: String): ParentHelpRequestResult
}

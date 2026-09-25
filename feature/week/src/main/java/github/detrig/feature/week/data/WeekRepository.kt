package github.detrig.feature.week.data

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.week.data.local.WeekDao
import github.detrig.feature.week.data.local.WeekStateEntity
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.week.domain.EarlyWeekEndResult
import github.detrig.feature.week.domain.WeekState
import github.detrig.feature.week.domain.PetDayEffects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import github.detrig.feature.gamestate.api.ProgressionApi
import github.detrig.feature.gamestate.domain.progression.GrantXpResult
import github.detrig.feature.gamestate.domain.progression.XpRewards
import github.detrig.feature.gamestate.domain.progression.XpSources
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.domain.isGoodWeeklyResult

internal class WeekRepository(
    private val dao: WeekDao,
    private val economyApi: EconomyApi,
    private val transactionRunner: RoomTransactionRunner,
    private val petDayEffects: PetDayEffects = PetDayEffects {},
    private val progressionApi: ProgressionApi,
    private val planningApi: PlanningApi,
) : WeekApi {
    override suspend fun initialize(): WeekState = transactionRunner.runInTransaction { ensureState() }

    override fun observeState(): Flow<WeekState> = dao.observeState().filterNotNull().map { WeekState(it.absoluteDay) }

    override suspend fun endDay(expectedAbsoluteDay: Long): EndDayResult = transactionRunner.runInTransaction {
        val current = ensureState()
        if (current.absoluteDay != expectedAbsoluteDay) return@runInTransaction EndDayResult.AlreadyAdvanced(current)
        val next = WeekState(Math.addExact(current.absoluteDay, 1))
        val allowance = if (next.dayOfWeek == 1) {
            rewardCompletedWeek(current.weekNumber)
            economyApi.grantWeeklyAllowance(next.weekNumber)
        } else null
        petDayEffects.afterSleep()
        check(dao.advance(expectedAbsoluteDay, next.absoluteDay) == 1)
        EndDayResult.Advanced(
            state = next,
            allowanceReceivedRub = allowance?.receivedRub ?: 0,
            allowanceGrossRub = allowance?.grossRub ?: 0,
            parentHelpRepaidRub = allowance?.parentHelpRepaidRub ?: 0,
        )
    }

    override suspend fun endWeekEarlyWithParentHelp(
        expectedAbsoluteDay: Long,
        minimumRequiredBalanceRub: Long,
    ): EarlyWeekEndResult = transactionRunner.runInTransaction {
        require(minimumRequiredBalanceRub > 0)
        val current = ensureState()
        if (current.absoluteDay != expectedAbsoluteDay) {
            return@runInTransaction EarlyWeekEndResult.AlreadyCompleted(current)
        }
        val economy = economyApi.getState()
        val activeParentHelp = economyApi.getParentHelp()
        if (economy.availableRub >= minimumRequiredBalanceRub || activeParentHelp == null) {
            return@runInTransaction EarlyWeekEndResult.NotNeeded(current)
        }
        val next = WeekState(current.weekNumber * WeekState.DAYS_PER_WEEK + 1)
        rewardCompletedWeek(current.weekNumber, includeGoodResult = false)
        val allowance = economyApi.grantWeeklyAllowance(next.weekNumber)
        check(dao.advance(expectedAbsoluteDay, next.absoluteDay) == 1)
        EarlyWeekEndResult.Completed(
            state = next,
            skippedDays = (WeekState.DAYS_PER_WEEK - current.dayOfWeek).toInt(),
            allowanceReceivedRub = allowance.receivedRub,
            allowanceGrossRub = allowance.grossRub,
            parentHelpRepaidRub = allowance.parentHelpRepaidRub,
        )
    }

    private suspend fun ensureState(): WeekState {
        dao.getState()?.let { return WeekState(it.absoluteDay) }
        dao.insertInitial(WeekStateEntity(absoluteDay = 1))
        return WeekState(checkNotNull(dao.getState()).absoluteDay)
    }

    private suspend fun rewardCompletedWeek(
        weekNumber: Long,
        includeGoodResult: Boolean = true,
    ) {
        progressionApi.grantXp(
            grantId = "week-completed:$weekNumber",
            profileId = CURRENT_PROFILE_ID,
            amount = XpRewards.WEEK_COMPLETED,
            source = XpSources.WEEK_COMPLETED,
        ).requireNoConflict()

        if (includeGoodResult && planningApi.getPlanProgress(weekNumber)?.isGoodWeeklyResult() == true) {
            progressionApi.grantXp(
                grantId = "good-week-result:$weekNumber",
                profileId = CURRENT_PROFILE_ID,
                amount = XpRewards.GOOD_WEEK_RESULT,
                source = XpSources.GOOD_WEEK_RESULT,
            ).requireNoConflict()
        }
    }

    private fun GrantXpResult.requireNoConflict() {
        check(this !is GrantXpResult.OperationIdConflict) { "Conflicting weekly XP grant" }
    }

    private companion object {
        const val CURRENT_PROFILE_ID = "current"
    }
}

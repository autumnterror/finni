package github.detrig.feature.savings.domain

import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.EconomyState
import github.detrig.feature.economy.domain.FinancialOperation
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.FinancialOperationType
import github.detrig.feature.economy.domain.FinancialSnapshot
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.PeriodicIncome
import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.domain.CategoryPlanProgress
import github.detrig.feature.planning.domain.PlanActualOperation
import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.feature.planning.domain.PlanPercentages
import github.detrig.feature.planning.domain.PlanProgressTone
import github.detrig.feature.planning.domain.RecordActualResult
import github.detrig.feature.planning.domain.WeeklyPlan
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.week.domain.EarlyWeekEndResult
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.week.domain.WeekState
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferWithoutGoalTest {

    @Test
    fun `transfer without a goal keeps money in savings without assigning a goal`() = runTest {
        val before = snapshot(availableRub = 100, savingsRub = 0)
        val after = snapshot(availableRub = 60, savingsRub = 40)
        val economyState = state(after)
        var savedContext: OperationContext? = null
        var recordedPlanActual: PlanActualOperation? = null
        val economy = Proxy.newProxyInstance(
            EconomyApi::class.java.classLoader,
            arrayOf(EconomyApi::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getSavingsHistory" -> emptyList<FinancialOperation>()
                "getGoals" -> emptyList<github.detrig.feature.economy.domain.SavingsGoal>()
                "transferToSavings" -> {
                    val context = args[2] as OperationContext
                    savedContext = context
                    val operation = FinancialOperation(
                        id = args[0] as String,
                        type = FinancialOperationType.TRANSFER_TO_SAVINGS,
                        amountRub = args[1] as Long,
                        timestampMillis = 1,
                        availableDeltaRub = -40,
                        savingsDeltaRub = 40,
                        debtDeltaRub = 0,
                        before = before,
                        after = after,
                        context = context,
                    )
                    FinancialOperationResult.Applied(operation, economyState)
                }
                else -> error("Unexpected EconomyApi call: ${method.name}")
            }
        } as EconomyApi
        val plan = WeeklyPlanProgress(
            plan = WeeklyPlan(weekNumber = 2, availableRub = 500, percentages = PlanPercentages.DEFAULT),
            categories = PlanCategory.entries.map { category ->
                CategoryPlanProgress(category, plannedRub = 100, actualRub = 0, tone = PlanProgressTone.ON_TRACK)
            },
        )
        val planning = Proxy.newProxyInstance(
            PlanningApi::class.java.classLoader,
            arrayOf(PlanningApi::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getPlanProgress" -> plan
                "recordActual" -> {
                    recordedPlanActual = args[0] as PlanActualOperation
                    RecordActualResult.Recorded
                }
                else -> error("Unexpected PlanningApi call: ${method.name}")
            }
        } as PlanningApi
        val learningApi = unusedProxy(LearningApi::class.java)
        val weekApi = object : WeekApi {
            private val week = WeekState(absoluteDay = 8)
            override suspend fun initialize() = week
            override fun observeState(): Flow<WeekState> = flowOf(week)
            override suspend fun endDay(expectedAbsoluteDay: Long): EndDayResult = error("Unused")
            override suspend fun endWeekEarlyWithParentHelp(
                expectedAbsoluteDay: Long,
                minimumRequiredBalanceRub: Long,
            ): EarlyWeekEndResult = error("Unused")
        }

        val learning = SavingsLearningInteractor(economy, weekApi, learningApi)
        val result = TransferToSavingsInteractor(
            economy = economy,
            planning = planning,
            learning = learning,
        )(operationId = "unassigned-deposit", goalId = null, amountRub = 40)

        assertEquals(40L, result.state.savingsRub)
        assertEquals("unassigned-savings", savedContext?.reasonId)
        assertEquals("source=savings;week=2;target=0", savedContext?.metadata)
        assertEquals(
            PlanActualOperation.SavingsContribution("unassigned-deposit", 2, 40),
            recordedPlanActual,
        )
        learning.recordTransfer((result as FinancialOperationResult.Applied).operation)
    }

    private fun snapshot(availableRub: Long, savingsRub: Long) =
        FinancialSnapshot(availableRub = availableRub, savingsRub = savingsRub, debtRub = 0)

    private fun state(snapshot: FinancialSnapshot) = EconomyState(
        availableRub = snapshot.availableRub,
        savingsRub = snapshot.savingsRub,
        debtRub = snapshot.debtRub,
        periodicIncome = PeriodicIncome(amountRub = 500, periodMillis = 7, nextAtMillis = 7),
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> unusedProxy(type: Class<T>): T = Proxy.newProxyInstance(
        type.classLoader,
        arrayOf(type),
    ) { _, method, _ -> error("Unexpected ${type.simpleName} call: ${method.name}") } as T
}

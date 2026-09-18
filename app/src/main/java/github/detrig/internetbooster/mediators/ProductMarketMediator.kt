package github.detrig.internetbooster.mediators

import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.productmarket.ProductMarketDependencies
import github.detrig.feature.productmarket.ProductMarketFeature
import github.detrig.feature.productmarket.api.ProductMarketHost
import github.detrig.feature.productmarket.api.MarketPaymentResult
import github.detrig.feature.productmarket.api.MarketSavingsGoalResult
import github.detrig.feature.productmarket.domain.MarketConfiguration
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.feature.savings.api.SavingsGoalDraft
import github.detrig.internetbooster.database.ProductMarketDatabaseModule
import github.detrig.products.DefaultProductCatalog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
internal class ProductMarketMediator(
    private val core: CoreComponent,
    private val database: ProductMarketDatabaseModule,
    private val economy: EconomyMediator,
    private val week: WeekMediator,
    private val planning: PlanningMediator,
    private val savings: SavingsMediator,
) {
    private val host = object : ProductMarketHost {
        override suspend fun preparePlayer() {
            economy.getApi().initialize()
            week.getApi().initialize()
        }
        override fun observeBalanceRub() = economy.getApi().observeState()
            .map { Math.toIntExact(it.availableRub) }.distinctUntilChanged()

        override suspend fun payForCart(tripId: String, totalRub: Long): MarketPaymentResult {
            if (totalRub == 0L) return MarketPaymentResult.Paid
            return when (val payment = economy.getApi().debit(
                operationId = "market:$tripId:purchase",
                amountRub = totalRub,
                context = OperationContext(reasonId = tripId, metadata = "source=product-market"),
            )) {
                is FinancialOperationResult.Applied,
                is FinancialOperationResult.AlreadyApplied -> {
                    val currentWeek = week.getApi().observeState().first()
                    if (planning.getApi().getPlanProgress(currentWeek.weekNumber) != null) {
                        planning.getApi().recordActual(
                            operationId = "market:$tripId:purchase",
                            weekNumber = currentWeek.weekNumber,
                            category = PlanCategory.MANDATORY,
                            amountRub = totalRub,
                        )
                    }
                    if (payment is FinancialOperationResult.Applied) MarketPaymentResult.Paid
                    else MarketPaymentResult.AlreadyPaid
                }
                is FinancialOperationResult.Rejected -> when (payment.reason) {
                    RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS -> MarketPaymentResult.InsufficientFunds(
                        (totalRub - payment.state.availableRub).coerceAtLeast(0),
                    )
                    else -> MarketPaymentResult.Rejected
                }
            }
        }

        override suspend fun saveCartAsGoal(tripId: String, totalRub: Long): MarketSavingsGoalResult = try {
            savings.getApi().createGoal(SavingsGoalDraft(
                id = "market-cart:$tripId",
                title = "Покупки из магазина",
                targetRub = totalRub,
                metadata = "source=product-market;tripId=$tripId",
            ))
            MarketSavingsGoalResult.GoalSaved
        } catch (_: IllegalArgumentException) {
            MarketSavingsGoalResult.Rejected
        }
    }
    fun init() {
        ProductMarketFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : ProductMarketDependencies {
                override fun host() = host
                override fun tripDao() = database.tripDao
                override fun productCatalog() = DefaultProductCatalog()
                override fun configuration() = MarketConfiguration()
                override fun applicationScope() = core.applicationScope
                override fun globalNavigator() = core.globalNavigator
            }
        }
    }
}

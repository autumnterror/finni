package github.detrig.feature.productmarket.api

sealed interface MarketPaymentResult {
    data object Paid : MarketPaymentResult
    data object AlreadyPaid : MarketPaymentResult
    data class InsufficientFunds(val missingRub: Long) : MarketPaymentResult
    data object Rejected : MarketPaymentResult
}

sealed interface MarketSavingsGoalResult {
    data object GoalSaved : MarketSavingsGoalResult
    data object Rejected : MarketSavingsGoalResult
}

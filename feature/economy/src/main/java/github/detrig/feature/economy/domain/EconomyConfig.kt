package github.detrig.feature.economy.domain

data class EconomyConfig(
    val initialAvailableRub: Long = 500,
    val initialSavingsRub: Long = 0,
    val periodicIncomeAmountRub: Long = 500,
    val weeklyAllowanceRub: Long = 1_000,
    val firstPeriodicIncomeDelayMillis: Long = 2L * 24 * 60 * 60 * 1_000,
    val periodicIncomePeriodMillis: Long = 7L * 24 * 60 * 60 * 1_000,
    val maximumDebtRub: Long = 1_000,
    val zeroBalanceHelpRub: Long = 500,
    val parentHelpOffers: List<ParentHelpOffer> = listOf(
        ParentHelpOffer("quick", receivedRub = 600, repaymentWeeks = 2, totalRepaymentRub = 720),
        ParentHelpOffer("steady", receivedRub = 600, repaymentWeeks = 3, totalRepaymentRub = 690),
        ParentHelpOffer("calm", receivedRub = 600, repaymentWeeks = 4, totalRepaymentRub = 660),
    ),
) {
    init {
        require(initialAvailableRub >= 0)
        require(initialSavingsRub >= 0)
        require(periodicIncomeAmountRub > 0)
        require(weeklyAllowanceRub > 0)
        require(firstPeriodicIncomeDelayMillis > 0)
        require(periodicIncomePeriodMillis > 0)
        require(maximumDebtRub > 0)
        require(zeroBalanceHelpRub > 0)
        require(parentHelpOffers.map { it.id }.distinct().size == parentHelpOffers.size)
        require(parentHelpOffers.all { it.totalRepaymentRub <= maximumDebtRub })
        require(parentHelpOffers.all {
            (it.totalRepaymentRub + it.repaymentWeeks - 1) / it.repaymentWeeks <= weeklyAllowanceRub
        })
    }

    fun initialState(nowMillis: Long) = EconomyState(
        availableRub = initialAvailableRub,
        savingsRub = initialSavingsRub,
        debtRub = 0,
        periodicIncome = PeriodicIncome(
            amountRub = periodicIncomeAmountRub,
            periodMillis = periodicIncomePeriodMillis,
            nextAtMillis = Math.addExact(nowMillis, firstPeriodicIncomeDelayMillis),
        ),
    )
}

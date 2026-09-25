package github.detrig.feature.room.presentation

import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.feature.planning.domain.PlanPercentages

/** Локальный черновик окна: в БД попадает только сохранённый план. */
internal data class PlanEditorState(
    val mandatory: Int = PlanPercentages.DEFAULT.mandatory,
    val wants: Int = PlanPercentages.DEFAULT.wants,
    val savings: Int = PlanPercentages.DEFAULT.savings,
) {
    val total: Int get() = mandatory + wants + savings
    val reserve: Int get() = PlanPercentages.TOTAL_PERCENT - total

    fun toPercentages() = PlanPercentages(mandatory, wants, savings)

    fun update(category: PlanCategory, requestedPercent: Int): PlanEditorState {
        val current = value(category)
        val selected = requestedPercent.coerceIn(0, PlanPercentages.TOTAL_PERCENT)
        if (selected == current) return this
        val first = otherFirst(category)
        val second = otherSecond(category)
        if (selected + first + second <= PlanPercentages.TOTAL_PERCENT) {
            return withValue(category, selected)
        }
        val remaining = PlanPercentages.TOTAL_PERCENT - selected
        val oldRemaining = first + second
        val nextFirst = if (oldRemaining == 0) remaining / 2 else remaining * first / oldRemaining
        val nextSecond = remaining - nextFirst
        return when (category) {
            PlanCategory.MANDATORY -> copy(mandatory = selected, wants = nextFirst, savings = nextSecond)
            PlanCategory.WANTS -> copy(wants = selected, mandatory = nextFirst, savings = nextSecond)
            PlanCategory.SAVINGS -> copy(savings = selected, mandatory = nextFirst, wants = nextSecond)
        }
    }

    fun updateReserve(requestedPercent: Int): PlanEditorState {
        val selected = requestedPercent.coerceIn(0, PlanPercentages.TOTAL_PERCENT)
        if (selected == reserve) return this

        val remaining = PlanPercentages.TOTAL_PERCENT - selected
        val source = if (total == 0) PlanPercentages.DEFAULT else toPercentages()
        val nextMandatory = remaining * source.mandatory / source.total
        val nextWants = remaining * source.wants / source.total
        val nextSavings = remaining - nextMandatory - nextWants
        return copy(
            mandatory = nextMandatory,
            wants = nextWants,
            savings = nextSavings,
        )
    }

    private fun withValue(category: PlanCategory, value: Int) = when (category) {
        PlanCategory.MANDATORY -> copy(mandatory = value)
        PlanCategory.WANTS -> copy(wants = value)
        PlanCategory.SAVINGS -> copy(savings = value)
    }

    private fun value(category: PlanCategory) = when (category) {
        PlanCategory.MANDATORY -> mandatory
        PlanCategory.WANTS -> wants
        PlanCategory.SAVINGS -> savings
    }

    private fun otherFirst(category: PlanCategory) = when (category) {
        PlanCategory.MANDATORY -> wants
        PlanCategory.WANTS, PlanCategory.SAVINGS -> mandatory
    }

    private fun otherSecond(category: PlanCategory) = when (category) {
        PlanCategory.MANDATORY -> savings
        PlanCategory.WANTS -> savings
        PlanCategory.SAVINGS -> wants
    }
}

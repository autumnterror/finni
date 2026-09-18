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

    fun toPercentages() = PlanPercentages(mandatory, wants, savings)

    fun update(category: PlanCategory, requestedPercent: Int): PlanEditorState {
        val current = value(category)
        val selected = requestedPercent.coerceIn(0, PlanPercentages.TOTAL_PERCENT)
        if (selected == current) return this
        val remaining = PlanPercentages.TOTAL_PERCENT - selected
        val first = otherFirst(category)
        val second = otherSecond(category)
        val oldRemaining = first + second
        val nextFirst = if (oldRemaining == 0) remaining / 2 else remaining * first / oldRemaining
        val nextSecond = remaining - nextFirst
        return when (category) {
            PlanCategory.MANDATORY -> copy(mandatory = selected, wants = nextFirst, savings = nextSecond)
            PlanCategory.WANTS -> copy(wants = selected, mandatory = nextFirst, savings = nextSecond)
            PlanCategory.SAVINGS -> copy(savings = selected, mandatory = nextFirst, wants = nextSecond)
        }
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

package github.detrig.feature.learning.domain

internal class LearningRuleEngine(
    private val rules: List<MetricRuleDefinition>,
) {
    fun rulesFor(actionType: LearningActionType): List<MetricRuleDefinition> =
        rules.filter { actionType in it.actionTypes }

    fun evaluate(
        rule: MetricRuleDefinition,
        qualifyingPeriods: List<Long>,
    ): MetricProgress {
        val sortedDistinctPeriods = qualifyingPeriods.distinct().sorted()
        val currentStreak = sortedDistinctPeriods.currentStreak()
        val progressSteps = rule.milestones
            .filter { milestone ->
                qualifyingPeriods.size >= milestone.requiredActions &&
                    sortedDistinctPeriods.size >= milestone.requiredDistinctPeriods &&
                    currentStreak >= milestone.requiredCurrentStreak
            }
            .maxOfOrNull { it.progressSteps }
            ?: 0

        return MetricProgress(
            metricId = rule.metricId,
            progressSteps = progressSteps,
            qualifyingRepeats = qualifyingPeriods.size,
            distinctPeriods = sortedDistinctPeriods.size,
            currentStreak = currentStreak,
            lastQualifyingPeriod = sortedDistinctPeriods.lastOrNull(),
        )
    }

    private fun List<Long>.currentStreak(): Int {
        if (isEmpty()) return 0
        var streak = 1
        for (index in 1 until size) {
            streak = if (this[index] == this[index - 1] + 1) streak + 1 else 1
        }
        return streak
    }
}

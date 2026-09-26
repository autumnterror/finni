package github.detrig.feature.learning.domain

internal class LearningRuleEngine(
    private val rules: List<MetricRuleDefinition>,
) {
    fun rulesFor(actionType: LearningActionType): List<MetricRuleDefinition> =
        rules.filter { actionType in it.actionTypes }

    fun evaluate(
        rule: MetricRuleDefinition,
        qualifyingPeriods: List<Long>,
        milestonePeriods: Map<Int, List<Long>> = emptyMap(),
    ): MetricProgress {
        val sortedDistinctPeriods = qualifyingPeriods.distinct().sorted()
        val currentStreak = sortedDistinctPeriods.currentStreak()
        val progressSteps = rule.milestones
            .filter { milestone ->
                val eligiblePeriods = if (milestone.qualifyingActionTypes == null) {
                    qualifyingPeriods
                } else {
                    requireNotNull(milestonePeriods[milestone.progressSteps]) {
                        "Missing eligible periods for ${rule.metricId} step ${milestone.progressSteps}"
                    }
                }
                val eligibleDistinctPeriods = eligiblePeriods.distinct().sorted()
                eligiblePeriods.size >= milestone.requiredActions &&
                    eligibleDistinctPeriods.size >= milestone.requiredDistinctPeriods &&
                    eligibleDistinctPeriods.currentStreak() >= milestone.requiredCurrentStreak
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

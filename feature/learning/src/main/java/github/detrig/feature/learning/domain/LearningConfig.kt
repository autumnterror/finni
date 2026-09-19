package github.detrig.feature.learning.domain

data class ProgressMilestone(
    val progressSteps: Int,
    val requiredActions: Int,
    val requiredDistinctPeriods: Int = 1,
    val requiredCurrentStreak: Int = 0,
) {
    init {
        require(progressSteps > 0)
        require(requiredActions > 0)
        require(requiredDistinctPeriods in 1..requiredActions)
        require(requiredCurrentStreak >= 0)
        require(requiredCurrentStreak <= requiredDistinctPeriods)
    }
}

data class MetricRuleDefinition(
    val metricId: String,
    val actionTypes: Set<LearningActionType>,
    val milestones: List<ProgressMilestone>,
) {
    init {
        require(metricId.isNotBlank())
        require(actionTypes.isNotEmpty())
        require(milestones.isNotEmpty())
        require(milestones.map { it.progressSteps }.distinct().size == milestones.size)
        require(milestones.zipWithNext().all { (left, right) ->
            left.progressSteps < right.progressSteps &&
                left.requiredActions <= right.requiredActions &&
                left.requiredDistinctPeriods <= right.requiredDistinctPeriods &&
                left.requiredCurrentStreak <= right.requiredCurrentStreak
        }) { "Metric milestones must be ordered and monotonic" }
    }
}

data class LearningConfig(
    val catalogVersion: Int = 1,
    val defaultAchievementXp: Int = 10,
    val achievementXpOverrides: Map<String, Int> = emptyMap(),
    val metricRules: List<MetricRuleDefinition> = emptyList(),
) {
    init {
        require(catalogVersion > 0)
        require(defaultAchievementXp > 0)
        require(achievementXpOverrides.values.all { it > 0 })
        require(metricRules.map { it.metricId }.distinct().size == metricRules.size) {
            "Only one metric rule may own a metric"
        }
    }

    fun xpFor(achievementId: String): Int =
        achievementXpOverrides[achievementId] ?: defaultAchievementXp
}

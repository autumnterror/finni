package github.detrig.feature.gamestate.domain.progression

enum class PetGrowthStage {
    BABY,
    TEEN,
    ADULT,
}

data class LevelDefinition(
    val level: Int,
    val totalXpRequired: Int,
)

data class GameProgress(
    val totalXp: Int,
    val level: Int,
    val petStage: PetGrowthStage,
    val currentLevelXp: Int,
    val nextLevelXp: Int?,
    val levelProgress: Float,
) {
    val isMaxLevel: Boolean get() = nextLevelXp == null
    val hasNewReactions: Boolean get() = level >= 2
    val hasNewOpportunities: Boolean get() = level >= 4
}

object ProgressionRules {
    val levels: List<LevelDefinition> = listOf(
        LevelDefinition(level = 1, totalXpRequired = 0),
        LevelDefinition(level = 2, totalXpRequired = 100),
        LevelDefinition(level = 3, totalXpRequired = 250),
        LevelDefinition(level = 4, totalXpRequired = 450),
        LevelDefinition(level = 5, totalXpRequired = 700),
    )

    init {
        require(levels.first().totalXpRequired == 0)
        require(levels.map { it.level } == (1..levels.size).toList())
        require(levels.zipWithNext().all { (left, right) ->
            left.totalXpRequired < right.totalXpRequired
        })
    }

    fun progress(totalXp: Int): GameProgress {
        require(totalXp >= 0) { "Total XP must not be negative" }
        val current = levels.last { totalXp >= it.totalXpRequired }
        val next = levels.getOrNull(current.level)
        val earnedAtLevel = totalXp - current.totalXpRequired
        val requiredAtLevel = next?.totalXpRequired?.minus(current.totalXpRequired)
        return GameProgress(
            totalXp = totalXp,
            level = current.level,
            petStage = stageForLevel(current.level),
            currentLevelXp = earnedAtLevel,
            nextLevelXp = requiredAtLevel,
            levelProgress = requiredAtLevel?.let { earnedAtLevel.toFloat() / it } ?: 1f,
        )
    }

    fun minimumXpForLevel(level: Int): Int =
        requireNotNull(levels.getOrNull(level - 1)) { "Unknown player level $level" }.totalXpRequired

    /** Divide the configured levels into three consecutive growth bands. */
    fun stageForLevel(level: Int, levelCount: Int = levels.size): PetGrowthStage {
        require(levelCount >= PetGrowthStage.entries.size)
        require(level in 1..levelCount)
        return PetGrowthStage.entries[(level - 1) * PetGrowthStage.entries.size / levelCount]
    }
}

object XpRewards {
    const val WEEK_COMPLETED = 40
    const val GOOD_WEEK_RESULT = 20
    const val SAVINGS_GOAL_REACHED = 40
    const val FINANCIAL_TASK_GUIDED = 5
    const val FINANCIAL_TASK_INDEPENDENT = 10
    const val ACHIEVEMENT_INTRODUCTION = 10
    const val ACHIEVEMENT_LEARNED = 20
    const val MINI_GAME = 2
}

object XpSources {
    const val WEEK_COMPLETED = "week_completed"
    const val GOOD_WEEK_RESULT = "good_week_result"
    const val SAVINGS_GOAL_REACHED = "savings_goal_reached"
    const val FINANCIAL_TASK_COMPLETED = "financial_task_completed"
    const val ACHIEVEMENT = "achievement"
    const val MINI_GAME = "mini_game"
}

object MiniGameXpPolicy {
    const val WEEKLY_CAP = 10

    fun reward(completedNaturally: Boolean, validActionCount: Int, activePlayMillis: Long): Int = when {
        !completedNaturally || validActionCount <= 0 || activePlayMillis <= 0 -> 0
        else -> XpRewards.MINI_GAME
    }

    fun rewardWithinWeek(earnedThisWeek: Int): Int =
        if (earnedThisWeek + XpRewards.MINI_GAME <= WEEKLY_CAP) XpRewards.MINI_GAME else 0
}

sealed interface GrantXpResult {
    val progress: GameProgress

    data class Granted(
        override val progress: GameProgress,
        val grantedXp: Int,
    ) : GrantXpResult

    data class AlreadyGranted(
        override val progress: GameProgress,
    ) : GrantXpResult

    data class OperationIdConflict(
        override val progress: GameProgress,
    ) : GrantXpResult
}

package github.detrig.feature.gamestate.domain.progression

enum class PetGrowthStage {
    BABY,
    EXPLORER,
    COMPANION,
}

data class LevelDefinition(
    val level: Int,
    val totalXpRequired: Int,
    val petStage: PetGrowthStage,
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
        LevelDefinition(level = 1, totalXpRequired = 0, petStage = PetGrowthStage.BABY),
        LevelDefinition(level = 2, totalXpRequired = 100, petStage = PetGrowthStage.BABY),
        LevelDefinition(level = 3, totalXpRequired = 250, petStage = PetGrowthStage.EXPLORER),
        LevelDefinition(level = 4, totalXpRequired = 450, petStage = PetGrowthStage.EXPLORER),
        LevelDefinition(level = 5, totalXpRequired = 700, petStage = PetGrowthStage.COMPANION),
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
            petStage = current.petStage,
            currentLevelXp = earnedAtLevel,
            nextLevelXp = requiredAtLevel,
            levelProgress = requiredAtLevel?.let { earnedAtLevel.toFloat() / it } ?: 1f,
        )
    }

    fun minimumXpForLevel(level: Int): Int =
        requireNotNull(levels.getOrNull(level - 1)) { "Unknown player level $level" }.totalXpRequired
}

object XpRewards {
    const val WEEK_COMPLETED = 80
    const val GOOD_WEEK_RESULT = 40
    const val SAVINGS_GOAL_REACHED = 100
    const val FINANCIAL_TASK_COMPLETED = 20
    const val ACHIEVEMENT_INTRODUCTION = 50
    const val ACHIEVEMENT_LEARNED = 20
    const val CONTENT_UNLOCKED = 50
    const val MINI_GAME = 5
    const val MINI_GAME_HIGH_ENGAGEMENT = 10
}

object XpSources {
    const val WEEK_COMPLETED = "week_completed"
    const val GOOD_WEEK_RESULT = "good_week_result"
    const val SAVINGS_GOAL_REACHED = "savings_goal_reached"
    const val FINANCIAL_TASK_COMPLETED = "financial_task_completed"
    const val ACHIEVEMENT = "achievement"
    const val CONTENT_UNLOCKED = "content_unlocked"
    const val MINI_GAME = "mini_game"
}

object MiniGameXpPolicy {
    private const val HIGH_ENGAGEMENT_MILLIS = 60_000L

    fun reward(completedNaturally: Boolean, validActionCount: Int, activePlayMillis: Long): Int = when {
        !completedNaturally || validActionCount <= 0 || activePlayMillis <= 0 -> 0
        activePlayMillis >= HIGH_ENGAGEMENT_MILLIS -> XpRewards.MINI_GAME_HIGH_ENGAGEMENT
        else -> XpRewards.MINI_GAME
    }
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

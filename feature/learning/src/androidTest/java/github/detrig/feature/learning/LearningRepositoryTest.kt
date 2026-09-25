package github.detrig.feature.learning

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.learning.api.LearningApiImpl
import github.detrig.feature.learning.api.LearningXpRewardGateway
import github.detrig.feature.learning.api.XpGrantResult
import github.detrig.feature.learning.data.LearningRepositoryImpl
import github.detrig.feature.learning.data.local.AchievementUnlockEntity
import github.detrig.feature.learning.data.local.AchievementXpOutboxEntity
import github.detrig.feature.learning.data.local.LearningActionEntity
import github.detrig.feature.learning.data.local.LearningDao
import github.detrig.feature.learning.data.local.LearningExplanationEntity
import github.detrig.feature.learning.data.local.LearningMetricOccurrenceEntity
import github.detrig.feature.learning.data.local.LearningMetricProgressEntity
import github.detrig.feature.learning.domain.AchievementStage
import github.detrig.feature.learning.domain.LearningAction
import github.detrig.feature.learning.domain.LearningActionType
import github.detrig.feature.learning.domain.LearningConfig
import github.detrig.feature.learning.domain.LearningMetricIds
import github.detrig.feature.learning.domain.LearningRuleEngine
import github.detrig.feature.learning.domain.MetricRuleDefinition
import github.detrig.feature.learning.domain.MvpAchievementCatalog
import github.detrig.feature.learning.domain.OpaqueLearningActionContext
import github.detrig.feature.learning.domain.ProgressMilestone
import github.detrig.feature.learning.domain.RecordLearningResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Database(
    entities = [
        LearningActionEntity::class,
        LearningMetricOccurrenceEntity::class,
        LearningMetricProgressEntity::class,
        AchievementUnlockEntity::class,
        LearningExplanationEntity::class,
        AchievementXpOutboxEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LearningTestDatabase : RoomDatabase() {
    abstract fun learningDao(): LearningDao
}

@RunWith(AndroidJUnit4::class)
class LearningRepositoryTest {
    private val actionType = LearningActionType("test.savings.goal.created")

    @Test
    fun actionsUnlockAchievementsOnceAndCreateIdempotentXpRewards() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, LearningTestDatabase::class.java).build()
        try {
            val config = LearningConfig(
                metricRules = listOf(
                    MetricRuleDefinition(
                        metricId = LearningMetricIds.SAVINGS_CREATE_GOAL,
                        actionTypes = setOf(actionType),
                        milestones = listOf(
                            ProgressMilestone(progressSteps = 1, requiredActions = 1),
                            ProgressMilestone(
                                progressSteps = 2,
                                requiredActions = 2,
                                requiredDistinctPeriods = 2,
                            ),
                        ),
                    )
                )
            )
            val catalog = MvpAchievementCatalog.create(config)
            val repository = LearningRepositoryImpl(
                dao = database.learningDao(),
                transactionRunner = RoomTransactionRunner(database),
                config = config,
                catalog = catalog,
                ruleEngine = LearningRuleEngine(config.metricRules),
            )

            val first = action("goal:1", period = 2, fingerprint = "goal=bike")
            val firstResult = repository.record(first) as RecordLearningResult.Processed
            assertEquals(1, firstResult.newlyUnlocked.size)
            assertEquals(AchievementStage.INTRODUCTION, firstResult.newlyUnlocked.single().definition.stage)
            assertTrue(repository.record(first) is RecordLearningResult.AlreadyProcessed)
            assertTrue(repository.record(first.copy(context = OpaqueLearningActionContext("goal=phone")))
                is RecordLearningResult.OperationIdConflict)

            val secondResult = repository.record(
                action("goal:2", period = 3, fingerprint = "goal=skate")
            ) as RecordLearningResult.Processed
            assertEquals(AchievementStage.LEARNED, secondResult.newlyUnlocked.single().definition.stage)

            val parentRows = repository.observeParentRows("current").first()
            assertEquals(2, parentRows.size)
            assertTrue(parentRows[0].text.startsWith("Ребёнок познакомился"))
            assertTrue(parentRows[1].text.startsWith("Ребёнок умеет"))
            assertEquals(2, repository.pendingXpRewards("current").size)

            assertTrue(repository.claimFirstExplanation("current", "savings.goal"))
            assertFalse(repository.claimFirstExplanation("current", "savings.goal"))

            val granted = mutableSetOf<String>()
            val api = LearningApiImpl(
                repository = repository,
                xpRewardGateway = object : LearningXpRewardGateway {
                    override suspend fun grantXp(
                        grantId: String,
                        profileId: String,
                        amount: Int,
                    ): XpGrantResult = if (granted.add(grantId)) {
                        XpGrantResult.Granted
                    } else {
                        XpGrantResult.AlreadyGranted
                    }
                },
            )
            assertEquals(2, api.deliverPendingXpRewards("current").delivered)
            assertEquals(0, api.deliverPendingXpRewards("current").delivered)

            repository.resetProfile("current")
            assertTrue(repository.observeParentRows("current").first().isEmpty())
            assertTrue(repository.claimFirstExplanation("current", "savings.goal"))
        } finally {
            database.close()
        }
    }

    @Test
    fun unknownActionDoesNotOccupyItsId() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, LearningTestDatabase::class.java).build()
        try {
            val config = LearningConfig()
            val repository = LearningRepositoryImpl(
                dao = database.learningDao(),
                transactionRunner = RoomTransactionRunner(database),
                config = config,
                catalog = MvpAchievementCatalog.create(config),
                ruleEngine = LearningRuleEngine(config.metricRules),
            )
            assertTrue(repository.record(action("future", 1, "v1"))
                is RecordLearningResult.UnsupportedAction)
            assertTrue(repository.record(action("future", 1, "v2"))
                is RecordLearningResult.UnsupportedAction)
        } finally {
            database.close()
        }
    }

    private fun action(id: String, period: Long, fingerprint: String) = LearningAction(
        actionId = id,
        profileId = "current",
        gamePeriod = period,
        type = actionType,
        context = OpaqueLearningActionContext(fingerprint),
        sourceOperationId = "operation:$id",
    )
}

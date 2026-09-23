package github.detrig.feature.planning

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.planning.data.PlanningRepository
import github.detrig.feature.planning.data.local.PlanActualOperationEntity
import github.detrig.feature.planning.data.local.PlanningDao
import github.detrig.feature.planning.data.local.WeeklyPlanEntity
import github.detrig.feature.planning.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Database(entities = [WeeklyPlanEntity::class, PlanActualOperationEntity::class], version = 1, exportSchema = false)
abstract class PlanningTestDatabase : RoomDatabase() { abstract fun planningDao(): PlanningDao }

@RunWith(AndroidJUnit4::class)
class PlanningRepositoryTest {
    @Test fun savedPlanIsImmutableAndActualOperationsAreIdempotent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, PlanningTestDatabase::class.java).build()
        try {
            val repository = PlanningRepository(
                database.planningDao(), RoomTransactionRunner(database), PlanningConfig(),
            )
            assertTrue(repository.savePlan(1, 500, PlanPercentages.DEFAULT) is SavePlanResult.Saved)
            val saved = repository.savePlan(2, 1_000, PlanPercentages.DEFAULT)
            assertTrue(saved is SavePlanResult.Saved)
            assertTrue(repository.savePlan(2, 1_200, PlanPercentages(20, 40, 40)) is SavePlanResult.AlreadySaved)
            assertEquals(RecordActualResult.Recorded,
                repository.recordActual("purchase:food:1", 2, PlanCategory.MANDATORY, 100))
            assertEquals(RecordActualResult.AlreadyRecorded,
                repository.recordActual("purchase:food:1", 2, PlanCategory.MANDATORY, 100))
            assertEquals(RecordActualResult.OperationIdConflict,
                repository.recordActual("purchase:food:1", 2, PlanCategory.WANTS, 100))
            assertEquals(100L, repository.getPlanProgress(2)!!.category(PlanCategory.MANDATORY).actualRub)
        } finally {
            database.close()
        }
    }
}

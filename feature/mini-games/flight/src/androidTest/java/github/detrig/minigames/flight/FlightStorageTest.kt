package github.detrig.minigames.flight

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.minigames.flight.data.*
import github.detrig.minigames.flight.domain.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@Database(entities = [FlightProgressEntity::class], version = 1, exportSchema = false)
abstract class FlightStorageTestDatabase : RoomDatabase() { abstract fun dao(): FlightDao }

@RunWith(AndroidJUnit4::class)
class FlightStorageTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: FlightStorageTestDatabase
    private lateinit var databaseName: String
    private lateinit var config: FlightConfig
    private lateinit var repository: RoomFlightRepository

    @Before fun setup() {
        databaseName = "flight-test-" + UUID.randomUUID() + ".db"
        config = FlightConfig.decode(context.assets.open("flight_balance.json").bufferedReader().use { it.readText() })
        reopen()
    }
    private fun reopen() {
        database = Room.databaseBuilder(context, FlightStorageTestDatabase::class.java, databaseName).build()
        repository = RoomFlightRepository(database.dao(), RoomTransactionRunner(database), config)
    }
    @After fun close() { database.close(); context.deleteDatabase(databaseName) }
    private fun session(id: String = "session") = FlightEngine(config).create(id, "profile", "pet", 1729, 100)

    @Test fun checkpointSurvivesReopenAndOldCallbacksCannotEraseResult() = runBlocking {
        val initial = session()
        val running = initial.copy(started = true, tick = 1500, score = 5, flapCount = 18)
        repository.begin(initial)
        repository.checkpoint(running)
        database.close()
        reopen()
        assertEquals(running, repository.load("profile").active)
        val ended = running.copy(outcome = FlightOutcome.LANDED)
        val result = repository.finish(ended, 200)
        repository.checkpoint(initial)
        assertEquals(result, repository.load("profile"))
        assertEquals(result, repository.finish(ended, 300))
        assertEquals(1, result.pendingEffects.size)
    }

    @Test fun resultAndEffectAcknowledgementSurviveReopen() = runBlocking {
        val s = session()
        repository.begin(s)
        repository.finish(s.copy(started = true, score = 12, tick = 2000, flapCount = 10,
            outcome = FlightOutcome.LANDED), 200)
        database.close(); reopen()
        assertEquals(1, repository.load("profile").pendingEffects.size)
        repository.acknowledgeEffect("profile", s.id, 3)
        repository.acknowledgeEffect("profile", s.id, 3)
        database.close(); reopen()
        val stored = repository.load("profile")
        assertTrue(stored.pendingEffects.isEmpty())
        assertEquals(3, stored.lastResult?.happinessDelta)
        assertEquals(12, stored.records[config.rulesVersion]?.score)
    }

    @Test fun failedFinalWriteDoesNotPublishRecord() = runBlocking {
        val s = session()
        repository.begin(s)
        val failingDao = object : FlightDao {
            override suspend fun find(profileId: String) = database.dao().find(profileId)
            override suspend fun save(entity: FlightProgressEntity) { error("disk full") }
        }
        val failing = RoomFlightRepository(failingDao, RoomTransactionRunner(database), config)
        assertTrue(runCatching { failing.finish(s.copy(score = 10, outcome = FlightOutcome.LANDED), 200) }.isFailure)
        assertTrue(repository.load("profile").records.isEmpty())
        assertEquals(s, repository.load("profile").active)
    }

    @Test fun unknownSaveIsNotOverwritten() = runBlocking {
        database.dao().save(FlightProgressEntity("profile", """{"schemaVersion":99,"profileId":"profile"}"""))
        val before = database.dao().find("profile")
        assertTrue(runCatching { repository.begin(session()) }.isFailure)
        assertEquals(before, database.dao().find("profile"))
    }

    @Test fun profileIsolationAndRuleMigrationPreserveHistory() = runBlocking {
        val s = session()
        repository.begin(s)
        repository.finish(s.copy(score = 8, outcome = FlightOutcome.LANDED), 300)
        val next = session("next")
        repository.begin(next)
        val updated = RoomFlightRepository(database.dao(), RoomTransactionRunner(database), config.copy(rulesVersion = config.rulesVersion + 1))
        val migrated = updated.interruptIncompatible("profile")
        assertNull(migrated.active)
        assertEquals(8, migrated.records[config.rulesVersion]?.score)
        assertTrue(repository.load("other").records.isEmpty())
    }

    @Test fun laterSessionAndActionCannotBeOverwrittenByOldCheckpoint() = runBlocking {
        val s = session()
        repository.begin(s)
        repository.checkpoint(s.copy(flapCount = 1, started = true))
        repository.checkpoint(s)
        assertEquals(1, repository.load("profile").active?.flapCount)
        repository.abandon("profile", s.id)
        val next = session("two")
        repository.begin(next)
        repository.checkpoint(s.copy(tick = 9999))
        assertEquals(next, repository.load("profile").active)
    }
}

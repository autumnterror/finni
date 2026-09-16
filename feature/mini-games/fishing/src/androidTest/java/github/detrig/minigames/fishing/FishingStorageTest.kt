package github.detrig.minigames.fishing

import androidx.room.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.minigames.fishing.data.*
import github.detrig.minigames.fishing.domain.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@Database(entities = [FishingProgressEntity::class], version = 1, exportSchema = false)
abstract class FishingTestDatabase : RoomDatabase() { abstract fun fishingDao(): FishingDao }

@RunWith(AndroidJUnit4::class)
class FishingStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: FishingTestDatabase
    private val name = "fishing-${UUID.randomUUID()}.db"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun open() = Room.databaseBuilder(context, FishingTestDatabase::class.java, name).build()
    private fun repository(version: Int = 5) = FishingRepositoryImpl(db.fishingDao(), RoomTransactionRunner(db), json, version)
    @Before fun before() { db = open() }
    @After fun after() { db.close(); context.deleteDatabase(name) }

    @Test fun concurrentUpdatesAndReopenKeepExactCountsAndProfileIsolation() = runBlocking {
        val repo = repository()
        (1..20).map { async(Dispatchers.IO) {
            repo.update("p") { p ->
                val count = (p.records.firstOrNull()?.count ?: 0) + 1
                p.copy(records = listOf(FishingRecord(4, 250, count, 1)))
            }
        } }.awaitAll()
        db.close(); db = open()
        assertEquals(20, repository().load("p").records.single().count)
        assertTrue(repository().load("other").records.isEmpty())
    }
    @Test fun failedTransactionKeepsPreviousDataAndCorruptPayloadIsNotReset() = runBlocking {
        val repo = repository()
        repo.update("p") { it.copy(tutorialDone = true) }
        assertTrue(runCatching { repo.update("p") { error("Write failed") } }.isFailure)
        assertTrue(repo.load("p").tutorialDone)
        db.fishingDao().write(FishingProgressEntity("bad", "{broken"))
        assertTrue(runCatching { repo.load("bad") }.isFailure)
        assertEquals("{broken", db.fishingDao().read("bad")!!.payload)
    }
    @Test fun rulesUpgradeDiscardsOnlyUnfinishedRound() = runBlocking {
        val config = json.decodeFromString<FishingConfig>(context.assets.open("fishing_balance.json").bufferedReader().readText())
        val session = FishingEngine(config).create("r", "p", 1, 1).copy(rulesVersion = 1)
        repository().update("p") { it.copy(session = session, tutorialDone = true,
            records = listOf(FishingRecord(1, 300, 1, 1))) }
        val upgraded = repository(5).load("p")
        assertNull(upgraded.session)
        assertTrue(upgraded.tutorialDone)
        assertEquals(1, upgraded.records.single().rulesVersion)
        assertTrue(upgraded.migrationNotice)
    }

    @Test fun legacyPayloadsDropAlbumAndCompetitionsButPreserveRecordsAndPendingEffects() = runBlocking {
        for (version in listOf(1, 2, 3, 4)) {
            val profile = "p$version"
            val payload = """{"schemaVersion":$version,"profileId":"$profile","tutorialDone":true,
                "preferences":{"inputMode":"HOLD"},"album":[{"speciesId":"carp","count":3}],"badges":["first_release"],
                "records":[{"rulesVersion":2,"grams":2000,"count":3,"achievedAtMillis":20}],
                "campaign":{"completedStages":2,"gear":{"rod":1}},
                "pendingEffects":[{"sessionId":"old","rulesVersion":2,"grams":250,"count":1,"completedAtMillis":10,
                "newRecord":true,"speciesIds":["crucian"],"newSpeciesIds":["crucian"],"newBadgeIds":[],"eligibleForHappiness":true}]}"""
            db.fishingDao().write(FishingProgressEntity(profile, payload))
            val migrated = repository().load(profile)
            assertEquals(5, migrated.schemaVersion)
            assertEquals(2000, migrated.records.single().grams)
            assertFalse(db.fishingDao().read(profile)!!.payload.contains("campaign"))
            assertEquals("old", migrated.pendingEffects.single().sessionId)
            assertFalse(db.fishingDao().read(profile)!!.payload.contains("inputMode"))
            assertFalse(db.fishingDao().read(profile)!!.payload.contains("album"))
            assertFalse(db.fishingDao().read(profile)!!.payload.contains("newSpeciesIds"))
            assertEquals(migrated, repository().load(profile))
        }
    }

    @Test fun worldCameraAndSlowerDescentSurviveDatabaseReopen() = runBlocking {
        val config = json.decodeFromString<FishingConfig>(context.assets.open("fishing_balance.json").bufferedReader().readText())
        val engine = FishingEngine(config)
        val session = engine.advance(engine.reel(engine.create("round", "p", 123, 100).copy(
            phase = FishingPhase.SEARCHING, hook = Hook(.08, .5))), 12.0)
        repository(5).update("p") { it.copy(session = session) }
        db.close(); db = open()
        val loaded = repository(5).load("p")
        assertEquals(session, loaded.session)
        assertTrue(loaded.session!!.hook.slowedDescent)
        assertTrue(loaded.session!!.cameraDepth > 0)
    }
}

package github.detrig.minigames.fishing

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.minigames.fishing.api.*
import github.detrig.minigames.fishing.data.FishingRepositoryImpl
import github.detrig.minigames.fishing.domain.*
import github.detrig.minigames.fishing.navigation.FishingRouter
import github.detrig.minigames.fishing.presentation.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FishingUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: FishingTestDatabase
    private lateinit var vm: FishingViewModel
    private lateinit var repo: FishingRepositoryImpl
    private val host = UiHost()

    @Before fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val config = json.decodeFromString<FishingConfig>(context.assets.open("fishing_balance.json").bufferedReader().readText())
        db = Room.inMemoryDatabaseBuilder(context, FishingTestDatabase::class.java).build()
        repo = FishingRepositoryImpl(db.fishingDao(), RoomTransactionRunner(db), json, config.rulesVersion)
        runBlocking { repo.update("test") { it.copy(tutorialDone = true, preferences = FishingPreferences()) } }
        ui.runOnUiThread {
            vm = FishingViewModel(FishingInteractor(repo, host, FishingEngine(config)) { 1000 }, object : FishingRouter {
                override fun open() = Unit
                override fun back() = Unit
            }, { 1000 })
        }
        ui.mainClock.autoAdvance = false
        ui.setContent { FinPetTheme { FishingScreen(vm) } }
        settle()
        ui.waitUntil(15_000) {
            ui.mainClock.advanceTimeByFrame()
            ui.onAllNodesWithTag("fishing_action").fetchSemanticsNodes().isNotEmpty()
        }
    }
    @After fun after() {
        ui.runOnUiThread { vm.perform(FishingViewEvent.Pause) }
        settle()
        ui.runOnUiThread { vm.viewModelScope.cancel() }
        db.close()
    }

    @Test fun entryAutomaticallyStartsOneRoundWithThreeSecondCountdown() {
        ui.onNodeWithTag("fishing_countdown").assertTextEquals("3")
        ui.onNodeWithTag("fishing_action").assertIsNotEnabled()
        ui.onNodeWithTag("fishing_start").assertDoesNotExist()
        ui.onNodeWithTag("fishing_campaign").assertDoesNotExist()
        val id = vm.scene!!.id
        assertEquals(FishingPhase.COUNTDOWN, vm.scene!!.phase)
        assertEquals(0.0, vm.scene!!.activeSeconds, .00001)
        ui.runOnUiThread { repeat(3) { vm.perform(FishingViewEvent.Load) } }
        assertEquals(id, vm.scene!!.id)
        assertEquals(id, runBlocking { repo.load("test") }.session!!.id)
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        settle()
        ui.onNodeWithTag("fishing_countdown").assertDoesNotExist()
        ui.onNodeWithTag("fishing_action").assertIsEnabled()
    }

    @Test fun holdingPullsAndReleaseRelaxesThenNewCastShowsZeroMeter() {
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        cast()
        advanceUntil { vm.scene?.phase == FishingPhase.FIGHTING }
        settle()
        val initial = vm.scene!!
        ui.onNodeWithTag("fishing_action").performTouchInput { down(center) }
        tick(250)
        assertTrue(vm.scene!!.pulling)
        assertTrue(vm.scene!!.hook.y < initial.hook.y)
        assertTrue(vm.scene!!.tension > initial.tension)
        ui.onNodeWithTag("fishing_action").performTouchInput { up() }
        val released = vm.scene!!
        tick(250)
        assertFalse(vm.scene!!.pulling)
        assertEquals(released.fightPullSeconds, vm.scene!!.fightPullSeconds, .00001)
        assertTrue(vm.scene!!.tension < released.tension)
        // Не подматываем: рыбка уходит, и старое значение не попадает в шкалу заброса.
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        settle()
        ui.onNodeWithTag("fishing_tension").assertDoesNotExist()
        ui.onNodeWithTag("fishing_power").assert(SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo,
            androidx.compose.ui.semantics.ProgressBarRangeInfo(0f, 0f..1f)))
        assertEquals(0.0, vm.scene!!.tension, 0.0)
        ui.runOnUiThread { vm.perform(FishingViewEvent.Pause) }
        settle()
        ui.onNodeWithTag("fishing_tap_mode").assertDoesNotExist()
    }

    @Test fun realScreenCastsCatchesAndRestoresWithoutAlbum() {
        screenshot("entry-countdown")
        settle()
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        ui.onNodeWithTag("fishing_time").assertTextEquals("1:15")
        val sceneBounds = ui.onNodeWithTag("fishing_action").fetchSemanticsNode().boundsInRoot
        val hudBounds = ui.onNodeWithTag("fishing_hud").fetchSemanticsNode().boundsInRoot
        assertTrue("Scene owns the main touch area", sceneBounds.height > hudBounds.height * 4)
        ui.onNodeWithTag("fishing_pause").assertDoesNotExist()
        cast()
        settle()
        assertEquals(1, vm.scene!!.validCasts)
        advanceUntil { vm.scene?.phase == FishingPhase.FIGHTING }
        screenshot("fight")
        reelUntilCaught()
        screenshot("catch")
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        ui.runOnUiThread { vm.perform(FishingViewEvent.Back) }
        settle()
        val paused = vm.scene!!
        tick(2000)
        assertEquals(paused.activeSeconds, vm.scene!!.activeSeconds, 0.0)
        screenshot("pause")
        ui.onNodeWithText("Альбом").assertDoesNotExist()
        assertEquals(1, runBlocking { repo.load("test") }.session!!.catches.size)
        assertEquals(0, host.effects.size)
    }

    @Test fun completedRoundShowsResultAndRewardsOnlyOnce() {
        runBlocking { repo.update("test") { it.copy(records = listOf(FishingRecord(vm.engine.config.rulesVersion, 10, 1, 0))) } }
        settle()
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        cast()
        advanceUntil { vm.scene?.phase == FishingPhase.FIGHTING }
        reelUntilCaught()
        repeat(160) {
            if (vm.state().value?.page != FishingPage.RESULTS) {
                ui.runOnUiThread { repeat(10) { vm.frame(.1) } }
                settle()
            }
        }
        assertEquals(FishingPage.RESULTS, vm.state().value?.page)
        settle()
        ui.onNodeWithTag("fishing_result_weight").assertExists()
        ui.onNodeWithText("Новый рекорд").assertExists()
        ui.onNodeWithTag("fishing_previous_record").assertTextEquals(formatFishingMass(10))
        val old = ui.onNodeWithTag("fishing_previous_record").fetchSemanticsNode().boundsInRoot
        val new = ui.onNodeWithTag("fishing_result_record").fetchSemanticsNode().boundsInRoot
        assertTrue(new.bottom <= old.top)
        ui.onNodeWithText("Альбом").assertDoesNotExist()
        assertEquals(1, host.effects.size)
        assertEquals(1, runBlocking { repo.load("test") }.records.size)
        screenshot("results")
        ui.mainClock.autoAdvance = true
        ui.onNodeWithTag("fishing_again").performScrollTo()
        ui.mainClock.autoAdvance = false
        ui.onNodeWithTag("fishing_again").performClick()
        settle()
        assertNotNull(vm.scene)
        assertEquals(1, host.effects.size)
    }

    @Test fun activityBackgroundingClearsHeldInputAndFreezesRound() {
        settle()
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        cast()
        advanceUntil { vm.scene?.phase == FishingPhase.FIGHTING }
        settle()
        ui.onNodeWithTag("fishing_action").performTouchInput { down(center) }
        tick(100)
        assertTrue(vm.scene!!.pulling)
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertTrue(vm.scene!!.paused)
        assertFalse(vm.scene!!.pulling)
        val time = vm.scene!!.activeSeconds
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        settle()
        tick(1000)
        assertEquals(time, vm.scene!!.activeSeconds, .00001)
    }

    @Test fun holdGestureCanCancelThenCastAndSettingsSurviveStart() {
        settle()
        settle()
        assertFalse(vm.state().value!!.progress!!.preferences.sound)
        assertFalse(vm.state().value!!.progress!!.preferences.haptics)
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        ui.onNodeWithTag("fishing_action").performTouchInput { down(center) }
        tick(200)
        ui.onNodeWithTag("fishing_action").performTouchInput { moveTo(androidx.compose.ui.geometry.Offset(-20f, center.y)); up() }
        settle()
        assertEquals(0, vm.scene!!.validCasts)
        assertEquals(FishingPhase.READY, vm.scene!!.phase)
        ui.onNodeWithTag("fishing_action").performTouchInput { down(center) }
        tick(200)
        ui.onNodeWithTag("fishing_action").performTouchInput { up() }
        settle()
        assertEquals(1, vm.scene!!.validCasts)
        assertFalse(vm.scene!!.pulling)
    }

    @Test fun lockedEntryRequestsTheRoomsOwnPreviewWithoutStartingARound() {
        val existingId = runBlocking { repo.load("test") }.session!!.id
        host.unlocked = false
        lateinit var fresh: FishingViewModel
        ui.runOnUiThread {
            fresh = FishingViewModel(FishingInteractor(repo, host, vm.engine) { 1000 }, object : FishingRouter {
                override fun open() = Unit
                override fun back() = Unit
            }, { 1000 })
            fresh.perform(FishingViewEvent.Load)
        }
        ui.waitUntil(5000) { fresh.state().value?.loading == false && fresh.state().value?.busy == false }
        assertEquals(1, host.unlockRequests)
        assertNull(fresh.scene)
        assertEquals(existingId, runBlocking { repo.load("test") }.session!!.id)
        ui.runOnUiThread { fresh.viewModelScope.cancel() }
    }

    @Test fun delayedFrameDoesNotPauseOrSkipSeveralSecondsOfPlay() {
        settle()
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        val before = vm.scene!!.activeSeconds
        ui.runOnUiThread { vm.frame(2.0) }
        assertFalse(vm.scene!!.paused)
        assertTrue(vm.scene!!.activeSeconds - before <= .10001)
        assertEquals(FishingPhase.READY, vm.scene!!.phase)
    }

    @Test fun rotationPreservesWorldAndResumesInLandscape() {
        settle()
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        cast()
        settle()
        val before = vm.scene!!
        val portraitActivity = ui.activity
        ui.runOnUiThread { ui.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        ui.waitUntil(10_000) { ui.activity !== portraitActivity && ui.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
        ui.activityRule.scenario.onActivity { it.setContent { FinPetTheme { FishingScreen(vm) } } }
        settle()
        ui.waitUntil(15_000) {
            ui.mainClock.advanceTimeByFrame()
            ui.onAllNodesWithText("Продолжить").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(vm.scene!!.paused)
        assertEquals(before.hook, vm.scene!!.hook)
        assertEquals(before.fish.map { it.grams }, vm.scene!!.fish.map { it.grams })
        ui.onNodeWithText("Продолжить").performClick()
        tick(2100)
        assertFalse(vm.scene!!.paused)
        screenshot("landscape")
    }


    @Test fun sceneHoldChargesAndEmptyHookLiftsOnRelease() {
        settle()
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        ui.onNodeWithTag("fishing_action").performTouchInput { down(center) }
        tick(1800)
        assertEquals(1.0, vm.scene!!.power, .001)
        ui.onNodeWithTag("fishing_action").performTouchInput { up() }
        settle()
        assertNull(vm.scene!!.targetSpawnId)
        advanceUntil { vm.scene?.phase == FishingPhase.SEARCHING }
        settle()
        ui.onNodeWithTag("fishing_action").assertIsEnabled()
        val count = vm.scene!!.reelCount
        ui.onNodeWithTag("fishing_action").performTouchInput { down(center) }
        assertTrue("Underwater press must be accepted", vm.scene!!.pulling)
        tick(250)
        assertTrue("Holding must survive recomposition", vm.scene!!.pulling)
        assertEquals(count, vm.scene!!.reelCount)
        ui.onNodeWithTag("fishing_action").performTouchInput { up() }
        settle()
        assertEquals("Release state: ${vm.scene}", count + 1, vm.scene!!.reelCount)
    }

    @Test fun deepCameraRestoresWithRareFishAndReturnsToSurface() {
        val engine = vm.engine
        val deep = engine.advance(engine.create("deep", "test", 17, 1000).copy(
            phase = FishingPhase.SEARCHING, validCasts = 1, hook = Hook(.08, .1)), 20.0)
        runBlocking { repo.update("test") { it.copy(session = engine.pause(deep)) } }
        ui.runOnUiThread {
            vm.viewModelScope.cancel()
            vm = FishingViewModel(FishingInteractor(repo, host, engine) { 1000 }, object : FishingRouter {
                override fun open() = Unit
                override fun back() = Unit
            }, { 1000 })
            ui.activity.setContent { FinPetTheme { FishingScreen(vm) } }
        }
        ui.waitUntil(15_000) {
            ui.mainClock.advanceTimeByFrame()
            vm.scene != null && ui.onAllNodesWithTag("fishing_countdown").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(deep.cameraDepth, vm.scene!!.cameraDepth, .0001)
        assertFalse(vm.scene!!.paused)
        assertTrue(vm.scene!!.resumeSeconds > 0)
        advanceUntil { vm.scene!!.resumeSeconds <= 0 }
        assertTrue(vm.scene!!.cameraDepth > 1.3)
        screenshot("depth")
        ui.onNodeWithText("Вернуть снасть").performClick()
        advanceUntil { vm.scene?.phase == FishingPhase.READY }
        tick(400)
        assertEquals(0.0, vm.scene!!.cameraDepth, .001)
        screenshot("surface")
    }


    private fun cast() {
        val initial = vm.scene!!
        val options = (1..30).map { it * 50L }.filter { millis ->
            val charged = vm.engine.advance(vm.engine.press(initial), millis / 1000.0)
            var trial = vm.engine.release(charged)
            repeat(140) { if (trial.phase != FishingPhase.FIGHTING) trial = vm.engine.advance(trial, .1) }
            trial.phase == FishingPhase.FIGHTING
        }
        val groups = mutableListOf<MutableList<Long>>()
        options.forEach { value ->
            if (groups.lastOrNull()?.lastOrNull() != value - 50) groups += mutableListOf<Long>()
            groups.last() += value
        }
        val group = groups.maxBy { it.size }
        val duration = (group.first() + group.last()) / 2
        ui.onNodeWithTag("fishing_action").performTouchInput { down(center) }
        settle()
        // Время заряда берём из модели: UI clock округляет кадры и включает ожидание Room.
        while (vm.scene!!.phaseSeconds + .001 < duration / 1000.0) {
            if (vm.state().value?.busy == true) settle()
            else ui.runOnUiThread { vm.frame(.01) }
        }
        ui.onNodeWithTag("fishing_action").performTouchInput { up() }
        settle()
    }
    private fun reelUntilCaught() {
        var holding = false
        repeat(180) {
            if ((vm.scene?.catches?.size ?: 0) > 0) {
                if (holding) ui.onNodeWithTag("fishing_action").performTouchInput { up() }
                return
            }
            val s = vm.scene ?: error("Round disappeared")
            if (s.phase == FishingPhase.FIGHTING) {
                val pull = !s.warning && !vm.engine.isBurst(s) && !vm.engine.burstSoon(s)
                if (pull != holding) {
                    if (pull) ui.onNodeWithTag("fishing_action").performTouchInput { down(center) }
                    else ui.onNodeWithTag("fishing_action").performTouchInput { up() }
                    holding = pull
                }
            } else if (s.phase != FishingPhase.CATCH_PENDING) error("Fish was lost: " + s.lastLoss)
            tick(100)
        }
        error("No delivery")
    }

    private fun settle() {
        ui.waitUntil(10_000) {
            ui.mainClock.advanceTimeByFrame()
            vm.state().value?.busy != true && vm.state().value?.loading != true
        }
        ui.mainClock.advanceTimeByFrame()
        ui.waitForIdle()
        assertNull(vm.state().value?.error)
    }
    private fun tick(ms: Long) {
        repeat((ms / 50).toInt().coerceAtLeast(1)) { ui.mainClock.advanceTimeBy(50); settle() }
    }
    private fun advanceUntil(maxSteps: Int = 300, condition: () -> Boolean) {
        repeat(maxSteps) { if (condition()) return; tick(50) }
        error("Phase did not reach expected state: ${vm.scene}; UI ${vm.state().value}")
    }
    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir("fishing-test"), "$name.png")
        file.outputStream().use { ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private class UiHost : FishingHost {
        val effects = mutableSetOf<String>()
        var unlocked = true
        var unlockRequests = 0
        override suspend fun environment() = FishingEnvironment("test", unlocked)
        override fun showUnlockPreview() { unlockRequests++ }
        override suspend fun applyPlayEffect(profileId: String, sessionId: String): Int { effects.add(sessionId); return 3 }
        override fun feedbackSettings() = FishingFeedbackSettings(sound = false, haptics = false)
        override fun saveFeedbackSettings(settings: FishingFeedbackSettings) = Unit
    }
}

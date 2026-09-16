package github.detrig.internetbooster

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.feature.gamestate.GameStateFeature
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomFishingNavigationTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test fun roomArtworkOpensFishingAndBackPreservesTheProfile() {
        ui.waitUntil(15_000) { ui.onAllNodesWithTag("house_scroll").fetchSemanticsNodes()
                .any { it.config.contains(SemanticsActions.ScrollBy) } }
        val before = runBlocking { GameStateFeature.getApi().initialize() }
        ui.onNodeWithTag("room_zone_fishing").performScrollTo()
        ui.waitForIdle()
        ui.onNodeWithTag("room_zone_fishing").assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Зона открыта"))
        val cameraBefore = ui.onNodeWithTag("house_scroll").fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange].value()
        ui.mainClock.autoAdvance = false
        ui.onNodeWithTag("room_zone_fishing").performClick()
        ui.waitUntil(15_000) {
            ui.mainClock.advanceTimeByFrame()
            ui.onAllNodesWithTag("fishing_action").fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithTag("fishing_start").assertDoesNotExist()
        ui.onNodeWithTag("fishing_campaign").assertDoesNotExist()
        ui.onNodeWithTag("fishing_time").assertIsDisplayed()
        ui.onNodeWithTag("fishing_countdown").assertTextEquals("3")
        InstrumentationRegistry.getInstrumentation().runOnMainSync { ui.activity.onBackPressedDispatcher.onBackPressed() }
        ui.mainClock.advanceTimeByFrame()
        ui.mainClock.autoAdvance = true
        ui.waitUntil(5000) { ui.onAllNodesWithText("В комнату").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("В комнату").performScrollTo().performClick()
        ui.onNodeWithText("Закончить").performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("house_scroll").fetchSemanticsNodes()
                .any { it.config.contains(SemanticsActions.ScrollBy) } }
        val cameraAfter = ui.onNodeWithTag("house_scroll").fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange].value()
        assertEquals("Camera must return to the fishing object", cameraBefore, cameraAfter, 2f)
        ui.onNodeWithTag("room_zone_fishing").assertIsDisplayed()
        val after = runBlocking { GameStateFeature.getApi().initialize() }
        assertEquals(before, after)
    }

}

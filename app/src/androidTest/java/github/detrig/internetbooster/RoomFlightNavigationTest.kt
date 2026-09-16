package github.detrig.internetbooster

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import github.detrig.feature.gamestate.GameStateFeature
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.test.espresso.Espresso
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomFlightNavigationTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test fun countdownAutomaticallyStartsAndNoInputEndsAttempt() {
        ui.waitUntil(15_000) {
            ui.onAllNodesWithTag("house_scroll").fetchSemanticsNodes()
                .any { it.config.contains(SemanticsActions.ScrollBy) }
        }
        ui.onNodeWithTag("room_zone_flight").performScrollTo()
        ui.onNodeWithTag("room_zone_flight").performClick()
        ui.waitUntil(15_000) {
            ui.onAllNodesWithTag("flight_result_score").fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithTag("flight_result_score").assertTextEquals("0")
        ui.onNodeWithText("Ещё раз").performClick()
        ui.waitUntil(5_000) {
            ui.onAllNodesWithTag("flight_countdown").fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithText("Полетели").assertDoesNotExist()
        ui.onNodeWithText("Пауза").assertDoesNotExist()
        Espresso.pressBack()
    }

    @Test fun openFlightFromRoomAndReturnWithoutPurchaseOrProfileChanges() {
        ui.waitUntil(15_000) {
            ui.onAllNodesWithTag("house_scroll").fetchSemanticsNodes()
                .any { it.config.contains(SemanticsActions.ScrollBy) }
        }
        val before = runBlocking { GameStateFeature.getApi().initialize() }
        ui.onNodeWithTag("room_zone_flight").performScrollTo()
        ui.onNodeWithText("Полёт", useUnmergedTree = true).assertDoesNotExist()
        ui.onNodeWithTag("room_zone_flight").assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Зона открыта"))
            .performClick()
        ui.waitUntil(15_000) {
            ui.onAllNodesWithTag("flight_scene").fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithText("Полёт питомца").assertDoesNotExist()
        ui.onNodeWithText("Полетели").assertDoesNotExist()
        ui.onNodeWithText("Пауза").assertDoesNotExist()
        val scene = ui.onNodeWithTag("flight_scene").fetchSemanticsNode().boundsInRoot
        val screen = ui.onNodeWithTag("flight_screen").fetchSemanticsNode().boundsInRoot
        assertEquals(screen, scene)
        val score = ui.onNodeWithTag("flight_score").fetchSemanticsNode().boundsInRoot
        assertTrue(score.left > screen.center.x && score.bottom < screen.center.y)
        Espresso.pressBack()
        ui.waitUntil(10_000) {
            ui.onAllNodesWithTag("room_zone_flight").fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithTag("room_zone_flight").assertIsDisplayed()
        assertEquals(before, runBlocking { GameStateFeature.getApi().initialize() })
    }
}

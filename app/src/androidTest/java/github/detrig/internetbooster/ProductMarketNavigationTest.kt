package github.detrig.internetbooster

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import github.detrig.feature.economy.EconomyFeature
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductMarketNavigationTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test fun exitButtonAsksForConfirmationAndReopeningRestoresCart() {
        val balance = runBlocking { EconomyFeature.getApi().initialize().availableRub }
        openMarket()
        resumeShoppingIfAtCheckout()
        waitFor("market_scene")
        waitForDescription("Баланс: $balance рублей")
        ui.onNodeWithContentDescription("Баланс: $balance рублей").assertExists()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ui.activity.requestedOrientation)
        val product = hasContentDescription("Взять:", substring = true) and hasClickAction()
        ui.waitUntil(20_000) {
            ui.mainClock.advanceTimeByFrame()
            ui.onAllNodes(product).fetchSemanticsNodes().isNotEmpty()
        }
        ui.onAllNodes(product).onFirst().performClick()
        ui.onNodeWithTag("market_cart").performClick()
        waitFor("market_cart_total")
        val total = ui.onNodeWithTag("market_cart_total").fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text
        ui.onNodeWithText("Продолжить").performClick()
        waitForAbsent("market_cart_dialog")
        val cartDescription = ui.onNodeWithTag("market_cart").fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].single()
        ui.onNodeWithTag("market_exit").performClick()
        waitFor("market_exit_dialog")
        ui.onNodeWithTag("market_cancel_exit").performClick()
        waitForAbsent("market_exit_dialog")
        ui.onNodeWithTag("market_scene").assertIsDisplayed()
        ui.onNodeWithTag("market_cart").assertContentDescriptionEquals(cartDescription)
        ui.onNodeWithTag("market_exit").performClick()
        waitFor("market_confirm_exit")
        ui.onNodeWithTag("market_confirm_exit").performClick()
        waitFor("room_product_market")
        waitForAbsent("product_market")
        openMarket()
        waitFor("market_scene")
        ui.onNodeWithTag("market_cart").assertContentDescriptionEquals(cartDescription)
        waitForDescription("Баланс: $balance рублей")
        ui.onNodeWithContentDescription("Баланс: $balance рублей").assertExists()
        ui.onNodeWithTag("market_cart").performClick()
        waitFor("market_cart_total")
        ui.onNodeWithTag("market_cart_total").assertTextEquals(total)
    }

    @Test fun systemBackClosesCartThenAsksBeforeLeavingMarket() {
        openMarket()
        resumeShoppingIfAtCheckout()
        waitFor("market_scene")
        ui.onNodeWithTag("market_cart").performClick()
        waitFor("market_cart_dialog")
        ui.runOnUiThread { ui.activity.onBackPressedDispatcher.onBackPressed() }
        waitForAbsent("market_cart_dialog")
        ui.onNodeWithTag("market_scene").assertIsDisplayed()
        ui.runOnUiThread { ui.activity.onBackPressedDispatcher.onBackPressed() }
        waitFor("market_exit_dialog")
        ui.onNodeWithTag("market_confirm_exit").performClick()
        waitFor("room_product_market")
        waitForAbsent("product_market")
        ui.onNodeWithTag("product_market").assertDoesNotExist()
    }

    private fun openMarket() {
        ui.mainClock.autoAdvance = true
        waitFor("room_product_market")
        ui.onNodeWithTag("room_product_market").performScrollTo()
        ui.mainClock.autoAdvance = false
        ui.onNodeWithTag("room_product_market").performClick()
    }

    private fun resumeShoppingIfAtCheckout() {
        waitFor("market_scene")
        // Сохранённый поход может быть завершён предыдущим запуском проверки.
        if (ui.onAllNodesWithText("Новый поход").fetchSemanticsNodes().isNotEmpty()) {
            ui.onNodeWithText("Новый поход").performClick()
            waitForAbsent("market_checkout")
        } else if (ui.onAllNodesWithText("Ещё проход").fetchSemanticsNodes().isNotEmpty()) {
            ui.onNodeWithText("Ещё проход").performClick()
            waitForAbsent("market_checkout")
        }
    }

    private fun waitFor(tag: String) {
        ui.waitUntil(20_000) {
            ui.mainClock.advanceTimeByFrame()
            ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() &&
                (tag != "room_product_market" || ui.onAllNodesWithTag("house_scroll").fetchSemanticsNodes()
                    .any { it.config.contains(SemanticsActions.ScrollBy) })
        }
    }

    private fun waitForAbsent(tag: String) {
        ui.waitUntil(20_000) {
            ui.mainClock.advanceTimeByFrame()
            ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitForDescription(description: String) {
        ui.waitUntil(20_000) {
            ui.mainClock.advanceTimeByFrame()
            ui.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

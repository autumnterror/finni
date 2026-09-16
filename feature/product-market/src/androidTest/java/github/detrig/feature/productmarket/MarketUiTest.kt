package github.detrig.feature.productmarket

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.designsystem.theme.FinPetThemePacks
import github.detrig.feature.productmarket.domain.*
import github.detrig.feature.productmarket.presentation.*
import github.detrig.products.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarketUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()
    private val config = MarketConfiguration()
    private val catalog = DefaultProductCatalog()
    private val rules = MarketRules(config, catalog) { "ui-trip" }
    private var state by mutableStateOf(MarketViewState(trip = rules.newTrip(),
        balanceRub = 420, loading = false, foreground = true))

    private fun content(width: Int = 360, height: Int = 640) {
        ui.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ui.setContent {
            FinPetTheme(FinPetThemePacks.wireframe) {
                MarketContent(state, config, catalog, null, { event ->
                    val trip = requireNotNull(state.trip)
                    state = when (event) {
                        is MarketViewEvent.Pick -> state.copy(trip = rules.pick(trip, event.instanceId, width.toDouble()))
                        is MarketViewEvent.Remove -> state.copy(trip = rules.remove(trip, event.productId))
                        MarketViewEvent.OpenCart -> state.copy(cartOpen = true)
                        MarketViewEvent.CloseCart -> state.copy(cartOpen = false)
                        MarketViewEvent.AnotherPass -> state.copy(trip = rules.anotherPass(trip))
                        MarketViewEvent.Finish -> state.copy(trip = rules.finish(trip))
                        else -> state
                    }
                }, Modifier.width(width.dp).height(height.dp))
            }
        }
    }

    @Test fun portraitShelfHasTwelveTargetsAndBalanceSharesHeaderRow() {
        content()
        val items = ui.onAllNodes(hasTestTagPrefix("product_0:0:")).fetchSemanticsNodes()
        assertEquals(12, items.size)
        items.forEach { assertTrue(it.boundsInRoot.width > 0 && it.boundsInRoot.height > 0) }
        val balance = ui.onNodeWithTag("market_balance").fetchSemanticsNode().boundsInRoot
        val list = ui.onNodeWithTag("list_apple").fetchSemanticsNode().boundsInRoot
        assertTrue(balance.left >= list.right)
        assertTrue(balance.center.y in list.top..list.bottom)
        ui.onNodeWithTag("market_exit").assertIsDisplayed().assertHasClickAction()
        ui.onNodeWithTag("price_0:0:0", useUnmergedTree = true).assertIsDisplayed().assertTextEquals("10 ₽")
        ui.onNodeWithTag("product_0:0:0")
            .assertContentDescriptionEquals("Взять: Морковь, 10 рублей за штуку")
        ui.onNodeWithTag("product_0:0:0").performClick()
        ui.onNodeWithTag("product_0:0:0").assertDoesNotExist()
        ui.onNodeWithContentDescription("Морковь: собрано 1 из 2").assertExists()
        assertEquals(420, state.balanceRub)
    }

    @Test fun cartPricesIncludeQuantitiesAndExtrasAndUpdateAfterRemoval() {
        content()
        ui.onNodeWithTag("product_0:0:0").performClick()
        ui.onNodeWithTag("product_0:0:4").performClick()
        ui.onNodeWithTag("product_0:0:2").performClick()
        ui.onNodeWithTag("market_cart").performClick()
        ui.onNodeWithTag("market_cart_dialog").assertIsDisplayed()
        ui.onNodeWithText("Сверх списка: 1").assertIsDisplayed()
        ui.onNodeWithText("2 × 10 ₽ = 20 ₽").assertIsDisplayed()
        ui.onNodeWithText("1 × 35 ₽ = 35 ₽").assertIsDisplayed()
        ui.onNodeWithTag("market_cart_total").assertIsDisplayed().assertTextEquals("Итого: 55 ₽")
        ui.onNodeWithTag("remove_berries").performClick()
        ui.onNodeWithTag("market_cart_total").assertTextEquals("Итого: 20 ₽")
        ui.onNodeWithTag("remove_carrot").performClick()
        ui.onNodeWithTag("market_cart_total").assertTextEquals("Итого: 10 ₽")
        ui.onNodeWithTag("remove_carrot").performClick()
        ui.onNodeWithText("В тележке пока пусто.").assertIsDisplayed()
        ui.onNodeWithTag("market_cart_total").assertTextEquals("Итого: 0 ₽")
        ui.onNodeWithText("Продолжить").performClick()
        ui.onNodeWithTag("market_cart_dialog").assertDoesNotExist()
        assertTrue(requireNotNull(state.trip).cart.isEmpty())
        assertEquals(420, state.balanceRub)
    }

    @Test fun counterMovesWithWorldAndPetDoesNotTeleportWhenSheetOpens() {
        state = state.copy(trip = rules.newTrip().copy(distance = config.endDistance - 60))
        content()
        val actorBefore = ui.onNodeWithTag("market_actor").fetchSemanticsNode().boundsInRoot
        val counterBefore = ui.onNodeWithTag("market_counter").fetchSemanticsNode().positionInRoot
        ui.runOnIdle {
            state = state.copy(trip = state.trip!!.copy(phase = MarketPhase.ARRIVED, distance = config.endDistance))
        }
        val counterArrived = ui.onNodeWithTag("market_counter").fetchSemanticsNode().positionInRoot
        assertTrue(counterArrived.x < counterBefore.x)
        assertEquals(counterBefore.y, counterArrived.y, .01f)
        ui.onNodeWithTag("market_checkout").assertDoesNotExist()
        ui.runOnIdle { state = state.copy(trip = state.trip!!.copy(phase = MarketPhase.CHECKOUT)) }
        assertEquals(actorBefore, ui.onNodeWithTag("market_actor").fetchSemanticsNode().boundsInRoot)
        assertEquals(counterArrived, ui.onNodeWithTag("market_counter").fetchSemanticsNode().positionInRoot)
        ui.onNodeWithTag("market_cart_total").assertIsDisplayed().assertTextEquals("Итого: 0 ₽")
        ui.onNodeWithTag("market_finish").assertIsDisplayed().performClick()
        ui.onNodeWithText("Поход завершён").assertIsDisplayed()
        ui.onNodeWithText("Деньги остались в кошельке.").assertIsDisplayed()
        ui.onNodeWithTag("market_cart_total").assertIsDisplayed().assertTextEquals("Итого: 0 ₽")
    }

    @Test fun smallPortraitCheckoutKeepsBothActionsAccessible() {
        state = state.copy(trip = rules.newTrip().copy(
            phase = MarketPhase.CHECKOUT, distance = config.endDistance,
            cart = catalog.products.associate { it.id to 2 },
        ))
        ui.setContent {
            FinPetTheme(FinPetThemePacks.wireframe) {
                MarketContent(state, config, catalog, null, {}, Modifier.width(320.dp).height(568.dp))
            }
        }
        ui.onNodeWithTag("market_finish").assertIsDisplayed()
        ui.onNodeWithText("Ещё проход").assertIsDisplayed()
        ui.onNodeWithTag("market_exit").assertIsDisplayed()
        ui.onNodeWithTag("market_cart_total").assertIsDisplayed().assertTextEquals("Итого: 580 ₽")
        ui.onNodeWithTag("market_cart_items").performScrollToNode(hasText("Готовая порция"))
        ui.onNodeWithText("2 × 80 ₽ = 160 ₽").assertIsDisplayed()
        ui.onNodeWithTag("market_cart_total").assertIsDisplayed().assertTextEquals("Итого: 580 ₽")
        val pet = ui.onNodeWithTag("market_actor").fetchSemanticsNode().boundsInRoot
        val sheet = ui.onNodeWithTag("market_checkout").fetchSemanticsNode().boundsInRoot
        assertTrue(sheet.bottom < pet.top)
    }

    @Test fun smallPortraitCartKeepsTotalAndContinueVisibleWhileItemsScroll() {
        state = state.copy(trip = rules.newTrip().copy(cart = catalog.products.associate { it.id to 2 }), cartOpen = true)
        content(width = 320, height = 568)
        ui.onNodeWithTag("market_cart_total").assertIsDisplayed().assertTextEquals("Итого: 580 ₽")
        ui.onNodeWithTag("market_cart_items").performScrollToNode(hasText("Готовая порция"))
        ui.onNodeWithText("2 × 80 ₽ = 160 ₽").assertIsDisplayed()
        ui.onNodeWithTag("market_cart_total").assertIsDisplayed()
        ui.onNodeWithText("Продолжить").assertIsDisplayed().performClick()
        ui.onNodeWithTag("market_cart_dialog").assertDoesNotExist()
    }

    private fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("tag starts with $prefix") {
        it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }
}

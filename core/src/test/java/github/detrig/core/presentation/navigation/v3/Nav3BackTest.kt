package github.detrig.core.presentation.navigation.v3

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class Nav3BackTest {
    private data class Route(val name: String) : NavKey

    @Test fun doubleCloseDoesNotExitFromRevealedRoot() {
        var now = 1_000L
        var finishes = 0
        val handler = Nav3NavigationHandlerImpl({ finishes++ }, { now })
        val stack = mutableListOf<NavKey>(Route("room"), Route("phone"))
        handler.addBackStack(DefaultNavGraphKey(stack), stack)
        handler.back()
        now += 100
        handler.back()
        assertEquals(listOf(Route("room")), stack)
        assertEquals(0, finishes)
        now += 500
        handler.back()
        assertEquals(1, finishes)
    }

    @Test fun doubleClosePopsOnlyOneScreenAndNewNavigationCanClose() {
        val handler = Nav3NavigationHandlerImpl({}, { 1_000L })
        val stack = mutableListOf<NavKey>(Route("room"), Route("shop"), Route("cart"))
        handler.addBackStack(DefaultNavGraphKey(stack), stack)
        repeat(2) { handler.back() }
        assertEquals(listOf(Route("room"), Route("shop")), stack)
        handler.navigate(Route("details"))
        handler.back()
        assertEquals(listOf(Route("room"), Route("shop")), stack)
    }
}

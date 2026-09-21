package github.detrig.feature.gamestate.domain

import github.detrig.feature.gamestate.domain.model.MiniGameAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class GameStateInitialConfigTest {

    @Test
    fun defaultsPreserveExistingPrototypeValues() {
        val session = GameStateInitialConfig().createState()
        assertEquals(20, session.pet.hunger)
        assertEquals(80, session.pet.thirst)
        assertEquals(70, session.pet.happiness)
        assertEquals(100, session.pet.health)
        assertEquals(1, session.playerLevel)
    }

    @Test
    fun initialValuesAndClockCanBeConfigured() {
        val config = GameStateInitialConfig(
            hunger = 0,
            thirst = 50,
            happiness = 100,
            health = 90,
            playerLevel = 2,
        )

        assertEquals(
            GameState(PetState(0, 50, 100, 90), 2),
            config.createState(),
        )
    }

    @Test
    fun rejectsInvalidInitialValues() {
        val invalidConfigs = listOf<() -> GameStateInitialConfig>(
            { GameStateInitialConfig(hunger = -1) },
            { GameStateInitialConfig(hunger = 101) },
            { GameStateInitialConfig(thirst = -1) },
            { GameStateInitialConfig(thirst = 101) },
            { GameStateInitialConfig(happiness = -1) },
            { GameStateInitialConfig(happiness = 101) },
            { GameStateInitialConfig(health = -1) },
            { GameStateInitialConfig(health = 101) },
            { GameStateInitialConfig(playerLevel = 0) },
        )

        invalidConfigs.forEach { createConfig ->
            assertThrows(IllegalArgumentException::class.java) { createConfig() }
        }
    }

    @Test
    fun fishingStartsLockedWhileFreeGamesRemainAvailable() {
        assertFalse(MiniGameAccess.isInitiallyOpen("fishing"))
        assertTrue(MiniGameAccess.isInitiallyOpen("ball"))
        assertTrue(MiniGameAccess.isInitiallyOpen("flight"))
    }

}

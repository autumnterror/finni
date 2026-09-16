package github.detrig.feature.gamestate.data.local

import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.gamestate.domain.PetState
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStateMapperTest {

    @Test
    fun roundTripPreservesEveryStoredValue() {
        val state = GameState(PetState(43, 61, 92, 88), 4)

        assertEquals(state, state.toEntity().toDomain())
        assertEquals("current", state.toEntity().id)
    }

    @Test
    fun existingEntityDoesNotReceiveNewGameDefaults() {
        val entity = GameStateEntity("current", 100, 40, 0, 55, 7)

        assertEquals(entity, entity.toDomain().toEntity())
    }
}

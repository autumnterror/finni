package github.detrig.feature.pet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ClothingStateTest {
    @Test
    fun `equipping replaces only the selected slot and never removes ownership`() {
        val state = ClothingState()
            .withPurchase("shirt")
            .withPurchase("hat")
            .withPurchase("vest")
            .withEquipped("body", "shirt")
            .withEquipped("head", "hat")
            .withEquipped("body", "vest")

        assertEquals(setOf("shirt", "hat", "vest"), state.ownedIds)
        assertEquals(mapOf("body" to "vest", "head" to "hat"), state.equippedBySlot)
        assertEquals(mapOf("head" to "hat"), state.withEquipped("body", null).equippedBySlot)
    }

    @Test
    fun `unowned item cannot be equipped`() {
        assertThrows(IllegalArgumentException::class.java) {
            ClothingState().withEquipped("body", "shirt")
        }
    }
}

package github.detrig.feature.pet.presentation

import github.detrig.feature.pet.domain.model.HamsterAppearance
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClothingLayerTest {
    @Test
    fun `body and hat variants follow current fur and ears`() {
        val fluffyWide = HamsterAppearance(fur = "fluffy", ears = "wide")
        val body = ClothingLayer("body.webp", 20, mapOf("fur" to "fluffy"))
        val hat = ClothingLayer("hat.webp", 60, mapOf("ears" to "wide"))
        val otherHat = ClothingLayer("hat_round.webp", 60, mapOf("ears" to "round"))

        assertTrue(body.fits(fluffyWide))
        assertTrue(hat.fits(fluffyWide))
        assertFalse(otherHat.fits(fluffyWide))
        assertFalse(body.fits(fluffyWide.copy(fur = "smooth")))
    }
}

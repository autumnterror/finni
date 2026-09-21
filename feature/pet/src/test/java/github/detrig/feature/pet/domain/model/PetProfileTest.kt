package github.detrig.feature.pet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PetProfileTest {

    @Test
    fun `profile is a hamster by default`() {
        val profile = PetProfile(
            name = "Финни",
            color = PetColor.Sunny,
        )

        assertEquals(PetSpecies.Hamster, profile.species)
    }

    @Test
    fun `profile rejects legacy species`() {
        assertThrows(IllegalArgumentException::class.java) {
            PetProfile(
                name = "Финни",
                species = PetSpecies.Cat,
                color = PetColor.Sunny,
            )
        }
    }
}

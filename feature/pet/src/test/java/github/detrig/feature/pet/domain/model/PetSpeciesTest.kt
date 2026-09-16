package github.detrig.feature.pet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PetSpeciesTest {

    @Test
    fun `all species round trip through stable storage keys`() {
        PetSpecies.entries.forEach { species ->
            assertEquals(species, PetSpecies.fromStorageKey(species.storageKey))
        }
    }

    @Test
    fun `rooster uses stable storage key`() {
        assertEquals(PetSpecies.Rooster, PetSpecies.fromStorageKey("rooster"))
    }
}

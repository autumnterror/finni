package github.detrig.feature.pet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetNameRulesTest {
    @Test
    fun `normalizes surrounding and repeated spaces`() {
        assertEquals("Мистер Кот", PetNameRules.normalize("  Мистер   Кот  "))
    }

    @Test
    fun `accepts letters numbers spaces and hyphen`() {
        assertNull(PetNameRules.validate("Кот-7"))
    }

    @Test
    fun `rejects empty name`() {
        assertEquals(PetNameValidationError.Empty, PetNameRules.validate("   "))
    }

    @Test
    fun `rejects unsupported characters`() {
        assertEquals(PetNameValidationError.InvalidCharacters, PetNameRules.validate("Кот!"))
        assertEquals(PetNameValidationError.InvalidCharacters, PetNameRules.validate("-"))
    }

    @Test
    fun `limits name to ten unicode code points`() {
        assertEquals("1234567890", PetNameRules.limit("12345678901"))
    }
}

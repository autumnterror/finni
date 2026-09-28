package github.detrig.feature.wardrobe.presentation

import github.detrig.feature.pet.api.ClothingItem
import github.detrig.feature.pet.domain.model.ClothingState
import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.PetProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class WardrobeViewStateTest {
    private val items = listOf(
        ClothingItem("owned-face", "Купленные очки", "face", 50),
        ClothingItem("available-body", "Новый жилет", "body", 80),
        ClothingItem("available-face", "Новые очки", "face", 70),
        ClothingItem("owned-body", "Купленная футболка", "body", 90),
    )
    private val profile = PetProfile(
        name = "Финни",
        color = PetColor.Sunny,
        clothing = ClothingState(ownedIds = setOf("owned-face", "owned-body")),
    )

    @Test
    fun `shop shows available items before owned items`() {
        val state = WardrobeViewState(
            loading = false,
            profile = profile,
            items = items,
            mode = WardrobeMode.SHOP,
        )

        assertEquals(
            listOf("available-body", "available-face", "owned-face", "owned-body"),
            state.visibleItems.map(ClothingItem::id),
        )
    }

    @Test
    fun `shop keeps available items first inside selected category`() {
        val state = WardrobeViewState(
            loading = false,
            profile = profile,
            items = items,
            mode = WardrobeMode.SHOP,
            category = "face",
        )

        assertEquals(
            listOf("available-face", "owned-face"),
            state.visibleItems.map(ClothingItem::id),
        )
    }

    @Test
    fun `wardrobe contains only owned items`() {
        val state = WardrobeViewState(
            loading = false,
            profile = profile,
            items = items,
            mode = WardrobeMode.OWNED,
        )

        assertEquals(
            listOf("owned-face", "owned-body"),
            state.visibleItems.map(ClothingItem::id),
        )
    }
}

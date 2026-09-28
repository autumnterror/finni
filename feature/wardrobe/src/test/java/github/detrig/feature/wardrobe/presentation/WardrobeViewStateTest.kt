package github.detrig.feature.wardrobe.presentation

import github.detrig.feature.pet.api.ClothingItem
import github.detrig.feature.pet.domain.model.ClothingState
import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.PetProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class WardrobeViewStateTest {
    private val items = listOf(
        ClothingItem("owned-body", "Жилет", "body", 90),
        ClothingItem("available-head", "Шляпа", "head", 100),
        ClothingItem("available-body", "Футболка", "body", 120),
        ClothingItem("owned-head", "Кепка", "head", 110),
        ClothingItem("available-body-2", "Свитер", "body", 150),
    )
    private val profile = PetProfile("Финни", color = PetColor.Sunny,
        clothing = ClothingState(ownedIds = setOf("owned-body", "owned-head")),
    )

    @Test fun wardrobeOpensOnOwnedClothingOnly() {
        val state = WardrobeViewState(items = items, profile = profile)
        assertEquals(WardrobeMode.OWNED, state.mode)
        assertEquals(listOf("owned-body", "owned-head"), state.visibleItems.map { it.id })
    }

    @Test fun shopKeepsOwnedItemsAfterAvailableItemsInCatalogOrder() {
        val state = WardrobeViewState(items = items, profile = profile, mode = WardrobeMode.SHOP)
        assertEquals(
            listOf("available-head", "available-body", "available-body-2", "owned-body", "owned-head"),
            state.visibleItems.map { it.id },
        )
    }

    @Test fun purchaseMovesItemToOwnedGroupWithinItsCategory() {
        val state = WardrobeViewState(items = items, profile = profile, mode = WardrobeMode.SHOP, category = "body")
        assertEquals(listOf("available-body", "available-body-2", "owned-body"), state.visibleItems.map { it.id })
        val afterPurchase = state.copy(profile = profile.copy(clothing = profile.clothing.withPurchase("available-body")))
        assertEquals(listOf("available-body-2", "owned-body", "available-body"), afterPurchase.visibleItems.map { it.id })
        assertEquals(
            listOf("owned-body", "available-body"),
            afterPurchase.copy(mode = WardrobeMode.OWNED).visibleItems.map { it.id },
        )
    }

    @Test fun emptyWardrobeStillAllowsBrowsingEntireShop() {
        val state = WardrobeViewState(items = items, profile = profile.copy(clothing = ClothingState()))
        assertEquals(emptyList<ClothingItem>(), state.visibleItems)
        assertEquals(items, state.copy(mode = WardrobeMode.SHOP).visibleItems)
    }
}

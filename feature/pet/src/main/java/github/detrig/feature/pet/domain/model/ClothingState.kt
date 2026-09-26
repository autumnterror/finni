package github.detrig.feature.pet.domain.model

data class ClothingState(
    val ownedIds: Set<String> = emptySet(),
    val equippedBySlot: Map<String, String> = emptyMap(),
) {
    fun withPurchase(itemId: String): ClothingState = copy(ownedIds = ownedIds + itemId)

    fun withEquipped(slot: String, itemId: String?): ClothingState {
        require(itemId == null || itemId in ownedIds)
        return copy(
            equippedBySlot = if (itemId == null) equippedBySlot - slot
                else equippedBySlot + (slot to itemId),
        )
    }
}

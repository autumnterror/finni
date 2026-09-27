package github.detrig.internetbooster.mediators

import github.detrig.feature.pet.api.ClothingItem
import github.detrig.products.GroceryCatalog
import kotlin.random.Random

/** One deterministic wish per game day at most. The pet's mood changes frequency, not obligation. */
internal object PetWishSchedule {
    enum class Kind { GROCERY, CLOTHING, FREE }

    data class Wish(
        val eventId: String,
        val kind: Kind,
        val title: String,
        val priceRub: Long = 0,
        val productId: String? = null,
    )

    private val groceryTitles = setOf(
        "Пирожное", "Лимонад", "Какао", "Клубничный коктейль", "Роллы", "Пицца",
    )
    private val clothingIds = setOf(
        "23_coral_bandana", "14_blue_cap", "31_round_glasses",
        "01_leaf_tee", "19_crown", "38_blue_backpack",
    )
    private val freeWishes = listOf(
        "Поиграть вместе", "Погладить питомца", "Пообщаться",
        "Надеть уже купленную одежду", "Сыграть в открытую мини-игру",
    )

    fun current(
        absoluteDay: Long,
        happiness: Int,
        clothing: List<ClothingItem>,
        ownedClothingIds: Set<String>,
        hasUnlockedGame: Boolean,
        safeOptionalRub: Long,
    ): Wish? {
        val chance = when {
            happiness >= 70 -> 0.15
            happiness >= 40 -> 0.35
            happiness >= 20 -> 0.65
            else -> 0.85
        }
        val random = Random(0x51A7L xor absoluteDay)
        if (random.nextDouble() >= chance) return null
        val food = GroceryCatalog().storefront.items
            .filter { it.title in groceryTitles }
            .map { Wish("", Kind.GROCERY, it.title, it.priceRub, it.id.value) }
        val clothes = clothing.filter { it.id in clothingIds && it.id !in ownedClothingIds }
            .map { Wish("", Kind.CLOTHING, it.name, it.priceRub, it.id) }
        val free = freeWishes.filter { title ->
            (title != "Надеть уже купленную одежду" || ownedClothingIds.isNotEmpty()) &&
                (title != "Сыграть в открытую мини-игру" || hasUnlockedGame)
        }.map { Wish("", Kind.FREE, it) }
        val candidates = (food + clothes + free).sortedWith(compareBy({ it.kind.name }, { it.title }))
        if (candidates.isEmpty()) return null
        val chosen = if (absoluteDay % 7L == 0L) {
            candidates.filter { it.priceRub > safeOptionalRub }
                .maxByOrNull { it.priceRub }
                ?: candidates[random.nextInt(candidates.size)]
        } else candidates[random.nextInt(candidates.size)]
        return chosen.copy(eventId = "pet-wish:$absoluteDay:${chosen.productId ?: chosen.title.hashCode()}")
    }
}

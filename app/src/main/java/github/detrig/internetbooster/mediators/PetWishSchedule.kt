package github.detrig.internetbooster.mediators

import github.detrig.feature.pet.api.ClothingItem
import github.detrig.products.GroceryCatalog
import github.detrig.products.FoodItem
import github.detrig.feature.room.domain.model.RoomWishObjectCandidate
import kotlin.random.Random

/** Creates one deterministic daily purchase/play wish; the board retains it for its own lifetime. */
internal object PetWishSchedule {
    // Temporary debug setting: show one optional wish every game day while the board is being tuned.
    // Reset this to false before release to restore the happiness-based schedule below.
    private const val DEBUG_FORCE_DAILY_WISH = true
    enum class Kind { GROCERY, CLOTHING, MINI_GAME, TOY, SAVINGS_TOP_UP }

    data class Wish(
        val eventId: String,
        val kind: Kind,
        val title: String,
        val priceRub: Long = 0,
        val productId: String? = null,
        val createdAbsoluteDay: Long,
        val expiresOnAbsoluteDayExclusive: Long,
        val createdAtMillis: Long = 0L,
    )

    private val groceryTitles = setOf("Пирожное", "Лимонад", "Какао", "Клубничный коктейль")
    private val clothingIds = setOf(
        "23_coral_bandana", "14_blue_cap", "31_round_glasses",
        "01_leaf_tee", "19_crown", "38_blue_backpack",
    )
    fun next(
        absoluteDay: Long,
        happiness: Int,
        clothing: List<ClothingItem>,
        ownedClothingIds: Set<String>,
        safeOptionalRub: Long,
        playableMiniGames: List<Pair<String, String>>,
        roomObjects: List<RoomWishObjectCandidate>,
        activeWishProductIds: Set<String>,
    ): Wish? {
        val scheduledChance = when {
            happiness >= 70 -> 0.15
            happiness >= 40 -> 0.35
            happiness >= 20 -> 0.65
            else -> 0.85
        }
        val chance = if (DEBUG_FORCE_DAILY_WISH) 1.0 else scheduledChance
        val random = Random(0x51A7L xor absoluteDay)
        if (random.nextDouble() >= chance) return null
        val food = GroceryCatalog().storefront.items
            .filterIsInstance<FoodItem>()
            .filter { it.title in groceryTitles && it.effects.satietyPercent == 0 }
            .filterNot { it.id.value in activeWishProductIds }
            .map {
                Wish("", Kind.GROCERY, it.title, it.priceRub, it.id.value,
                    absoluteDay, absoluteDay + 1L)
            }
        val clothes = clothing.filter { it.id in clothingIds && it.id !in ownedClothingIds }
            .filterNot { it.id in activeWishProductIds }
            .map {
                Wish("", Kind.CLOTHING, it.name, it.priceRub, it.id,
                    absoluteDay, absoluteDay + random.nextInt(2, 4))
            }
        val games = playableMiniGames
            .filterNot { it.first in activeWishProductIds }
            .map {
                Wish("", Kind.MINI_GAME, it.second, 0L, it.first,
                    absoluteDay, absoluteDay + 1L)
            }
        val toys = roomObjects
            .filterNot { it.id in activeWishProductIds }
            .map {
                Wish("", Kind.TOY, it.title, it.priceRub, it.id,
                    absoluteDay, absoluteDay + random.nextInt(2, 4))
            }
        val candidates = (food + clothes + games + toys)
            .sortedWith(compareBy({ it.kind.name }, { it.title }))
        if (candidates.isEmpty()) return null
        val chosen = if (absoluteDay % 7L == 0L) {
            candidates.filter { it.priceRub > safeOptionalRub }
                .maxByOrNull { it.priceRub }
                ?: candidates[random.nextInt(candidates.size)]
        } else {
            // Draw a category first so a large interior catalog does not crowd out food, clothing, and play.
            val groups = candidates.groupBy { it.kind }.values.toList()
            val group = groups[random.nextInt(groups.size)]
            group[random.nextInt(group.size)]
        }
        return chosen.copy(
            eventId = "pet-wish:$absoluteDay:${chosen.productId ?: chosen.title.hashCode()}",
        )
    }
}

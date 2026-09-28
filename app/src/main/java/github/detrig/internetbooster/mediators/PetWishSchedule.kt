package github.detrig.internetbooster.mediators

import github.detrig.feature.pet.api.ClothingItem
import github.detrig.products.GroceryCatalog
import github.detrig.products.FoodItem
import github.detrig.feature.room.domain.model.RoomWishObjectCandidate
import kotlin.random.Random

/** Chooses a wish on a day assigned by the shared room event schedule. */
internal object PetWishSchedule {
    enum class Kind { GROCERY, CLOTHING, MINI_GAME, TOY, SAVINGS_TOP_UP, SAVINGS_GOAL, SAVED_GAME }

    data class Wish(
        val eventId: String,
        val kind: Kind,
        val title: String,
        val priceRub: Long = 0,
        val productId: String? = null,
        val createdAbsoluteDay: Long,
        val expiresOnAbsoluteDayExclusive: Long,
        val createdAtMillis: Long = 0L,
        val completedOnAbsoluteDay: Long? = null,
    ) {
        val isCompleted: Boolean get() = completedOnAbsoluteDay != null
    }

    private val groceryTitles = setOf("Пирожное", "Лимонад", "Какао", "Клубничный коктейль")
    private val clothingIds = setOf(
        "23_coral_bandana", "14_blue_cap", "31_round_glasses",
        "01_leaf_tee", "19_crown", "38_blue_backpack",
    )
    fun next(
        absoluteDay: Long,
        clothing: List<ClothingItem>,
        ownedClothingIds: Set<String>,
        playableMiniGames: List<Pair<String, String>>,
        roomObjects: List<RoomWishObjectCandidate>,
        activeWishProductIds: Set<String>,
    ): Wish? {
        val random = Random(0x51A7L xor absoluteDay)
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
        // Play is the most common wish. Renormalize only across available categories.
        val groups = candidates.groupBy { it.kind }.values.toList()
        val weights = groups.map { group ->
            when (group.first().kind) {
                Kind.MINI_GAME -> 60
                Kind.GROCERY -> 20
                Kind.CLOTHING, Kind.TOY -> 10
                else -> error("Unexpected random wish category")
            }
        }
        var roll = random.nextInt(weights.sum())
        val groupIndex = weights.indexOfFirst { weight ->
            (roll < weight).also { if (!it) roll -= weight }
        }
        val group = groups[groupIndex]
        val chosen = group[random.nextInt(group.size)]
        return chosen.copy(
            eventId = "pet-wish:$absoluteDay:${chosen.productId ?: chosen.title.hashCode()}",
        )
    }
}

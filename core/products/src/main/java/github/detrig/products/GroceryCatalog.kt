package github.detrig.products

object GroceryStoreIds {
    val Store = StoreId("store.grocery")
}

object GroceryCategoryIds {
    val Products = StoreCategoryId("store.grocery.category.products")
    val Meals = StoreCategoryId("store.grocery.category.meals")
    val Drinks = StoreCategoryId("store.grocery.category.drinks")
}

/** Public item ids are shared by the shop, cart, inventory and feeding flows. */
object GroceryItemIds {
    val Apple = itemId("apple")
    val Banana = itemId("banana")
    val Pear = itemId("pear")
    val Orange = itemId("orange")
    val Lemon = itemId("lemon")
    val Strawberry = itemId("strawberry")
    val Grapes = itemId("grapes")
    val Watermelon = itemId("watermelon")
    val Peach = itemId("peach")
    val Kiwi = itemId("kiwi")
    val Carrot = itemId("carrot")
    val Tomato = itemId("tomato")
    val Cucumber = itemId("cucumber")
    val Broccoli = itemId("broccoli")
    val Potato = itemId("potato")
    val Corn = itemId("corn")
    val Bread = itemId("bread")
    val Cheese = itemId("cheese")
    val Egg = itemId("egg")
    val Yogurt = itemId("yogurt")

    val Salad = itemId("salad")
    val Soup = itemId("soup")
    val Sandwich = itemId("sandwich")
    val FishWithLemon = itemId("fish_with_lemon")
    val Pizza = itemId("pizza")
    val Pasta = itemId("pasta")
    val Omelet = itemId("omelet")
    val Pancakes = itemId("pancakes")
    val Rolls = itemId("rolls")
    val Cake = itemId("cake")

    val Milk = itemId("milk")
    val OrangeJuice = itemId("orange_juice")
    val AppleJuice = itemId("apple_juice")
    val Lemonade = itemId("lemonade")
    val BerrySmoothie = itemId("berry_smoothie")
    val StrawberryCocktail = itemId("strawberry_cocktail")
    val Cocoa = itemId("cocoa")
    val Tea = itemId("tea")
    val Compote = itemId("compote")

    private fun itemId(key: String) = ProductId("store.grocery.item.$key")
}

/**
 * Initial balanced grocery configuration.
 *
 * Prices and effects live together as catalog data so future balancing does not touch shop UI.
 * Image keys are logical manifest keys; they are deliberately not Android drawable ids.
 */
class GroceryCatalog : SellableCatalog<FoodItem> {
    override val storefront: StorefrontDefinition<FoodItem> = StorefrontDefinition(
        storeId = GroceryStoreIds.Store,
        title = "Продуктовый",
        allItemsLabel = "Все",
        categories = listOf(
            StoreCategory(GroceryCategoryIds.Products, "Продукты"),
            StoreCategory(GroceryCategoryIds.Meals, "Блюда"),
            StoreCategory(GroceryCategoryIds.Drinks, "Напитки"),
        ),
        items = PRODUCTS + MEALS + DRINKS,
        gridLayout = StoreGridLayout(columns = 2),
        receiptTypeCode = StoreReceiptTypeCodes.Grocery,
    )

    companion object {
        private val PRODUCTS = listOf(
            food(GroceryItemIds.Apple, "Яблоко", 30, GroceryCategoryIds.Products, 18),
            food(GroceryItemIds.Banana, "Банан", 30, GroceryCategoryIds.Products, 22),
            food(GroceryItemIds.Pear, "Груша", 25, GroceryCategoryIds.Products, 20),
            food(GroceryItemIds.Orange, "Апельсин", 25, GroceryCategoryIds.Products, 18),
            food(GroceryItemIds.Lemon, "Лимон", 15, GroceryCategoryIds.Products, 8),
            food(GroceryItemIds.Strawberry, "Клубника", 35, GroceryCategoryIds.Products, 16, happiness = 2),
            food(GroceryItemIds.Grapes, "Виноград", 40, GroceryCategoryIds.Products, 18, happiness = 2),
            food(GroceryItemIds.Watermelon, "Арбуз", 35, GroceryCategoryIds.Products, 22, happiness = 1),
            food(GroceryItemIds.Peach, "Персик", 30, GroceryCategoryIds.Products, 18, happiness = 1),
            food(GroceryItemIds.Kiwi, "Киви", 30, GroceryCategoryIds.Products, 16, happiness = 1),
            food(GroceryItemIds.Carrot, "Морковь", 20, GroceryCategoryIds.Products, 14),
            food(GroceryItemIds.Tomato, "Помидор", 20, GroceryCategoryIds.Products, 14),
            food(GroceryItemIds.Cucumber, "Огурец", 20, GroceryCategoryIds.Products, 12),
            food(GroceryItemIds.Broccoli, "Брокколи", 25, GroceryCategoryIds.Products, 18),
            food(GroceryItemIds.Potato, "Картофель", 25, GroceryCategoryIds.Products, 22),
            food(GroceryItemIds.Corn, "Кукуруза", 30, GroceryCategoryIds.Products, 20, happiness = 1),
            food(GroceryItemIds.Bread, "Хлеб", 30, GroceryCategoryIds.Products, 25),
            food(GroceryItemIds.Cheese, "Сыр", 45, GroceryCategoryIds.Products, 25, happiness = 2),
            food(GroceryItemIds.Egg, "Яйцо", 30, GroceryCategoryIds.Products, 24),
            food(GroceryItemIds.Yogurt, "Йогурт", 40, GroceryCategoryIds.Products, 25, happiness = 3),
        )

        private val MEALS = listOf(
            food(GroceryItemIds.Salad, "Салат", 50, GroceryCategoryIds.Meals, 38, happiness = 1),
            food(GroceryItemIds.Soup, "Суп", 60, GroceryCategoryIds.Meals, 52, happiness = 2),
            food(GroceryItemIds.Sandwich, "Сэндвич", 55, GroceryCategoryIds.Meals, 48, happiness = 2),
            food(GroceryItemIds.FishWithLemon, "Рыба с лимоном", 85, GroceryCategoryIds.Meals, 58, happiness = 7),
            food(GroceryItemIds.Pizza, "Пицца", 95, GroceryCategoryIds.Meals, 55, happiness = 10),
            food(GroceryItemIds.Pasta, "Паста", 80, GroceryCategoryIds.Meals, 55, happiness = 6),
            food(GroceryItemIds.Omelet, "Омлет", 60, GroceryCategoryIds.Meals, 50, happiness = 2),
            food(GroceryItemIds.Pancakes, "Блинчики", 75, GroceryCategoryIds.Meals, 45, happiness = 8),
            food(GroceryItemIds.Rolls, "Роллы", 110, GroceryCategoryIds.Meals, 50, happiness = 13),
            food(GroceryItemIds.Cake, "Пирожное", 65, GroceryCategoryIds.Meals, 0, happiness = 20),
        )

        private val DRINKS = listOf(
            food(GroceryItemIds.Milk, "Молоко", 35, GroceryCategoryIds.Drinks, 20),
            food(GroceryItemIds.OrangeJuice, "Апельсиновый сок", 35, GroceryCategoryIds.Drinks, 12, happiness = 2),
            food(GroceryItemIds.AppleJuice, "Яблочный сок", 30, GroceryCategoryIds.Drinks, 12, happiness = 2),
            food(GroceryItemIds.Lemonade, "Лимонад", 35, GroceryCategoryIds.Drinks, 0, happiness = 12),
            food(GroceryItemIds.BerrySmoothie, "Ягодный смузи", 45, GroceryCategoryIds.Drinks, 22, happiness = 3),
            food(GroceryItemIds.StrawberryCocktail, "Клубничный коктейль", 55, GroceryCategoryIds.Drinks, 0, happiness = 18),
            food(GroceryItemIds.Cocoa, "Какао", 40, GroceryCategoryIds.Drinks, 0, happiness = 15),
            food(GroceryItemIds.Tea, "Чай", 20, GroceryCategoryIds.Drinks, 4),
            food(GroceryItemIds.Compote, "Компот", 30, GroceryCategoryIds.Drinks, 10, happiness = 1),
        )

        private fun food(
            id: ProductId,
            title: String,
            priceRub: Long,
            categoryId: StoreCategoryId,
            satiety: Int,
            happiness: Int = 0,
        ) = FoodItem(
            id = id,
            title = title,
            imageKey = ProductImageKey(id.value.replace(".item.", ".image.")),
            priceRub = priceRub,
            categoryId = categoryId,
            effects = FoodEffects(
                satietyPercent = satiety,
                happinessPoints = happiness,
            ),
        )
    }
}

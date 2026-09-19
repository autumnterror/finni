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
        gridLayout = StoreGridLayout(columns = 3),
        receiptTypeCode = StoreReceiptTypeCodes.Grocery,
    )

    companion object {
        private val PRODUCTS = listOf(
            food(GroceryItemIds.Apple, "Яблоко", 15, GroceryCategoryIds.Products, 12),
            food(GroceryItemIds.Banana, "Банан", 20, GroceryCategoryIds.Products, 18),
            food(GroceryItemIds.Pear, "Груша", 18, GroceryCategoryIds.Products, 14),
            food(GroceryItemIds.Orange, "Апельсин", 18, GroceryCategoryIds.Products, 12),
            food(GroceryItemIds.Lemon, "Лимон", 10, GroceryCategoryIds.Products, 6),
            food(GroceryItemIds.Strawberry, "Клубника", 22, GroceryCategoryIds.Products, 10, happiness = 1),
            food(GroceryItemIds.Grapes, "Виноград", 28, GroceryCategoryIds.Products, 14, happiness = 1),
            food(GroceryItemIds.Watermelon, "Арбуз", 25, GroceryCategoryIds.Products, 18),
            food(GroceryItemIds.Peach, "Персик", 20, GroceryCategoryIds.Products, 12),
            food(GroceryItemIds.Kiwi, "Киви", 22, GroceryCategoryIds.Products, 11),
            food(GroceryItemIds.Carrot, "Морковь", 10, GroceryCategoryIds.Products, 8),
            food(GroceryItemIds.Tomato, "Помидор", 12, GroceryCategoryIds.Products, 8),
            food(GroceryItemIds.Cucumber, "Огурец", 10, GroceryCategoryIds.Products, 7),
            food(GroceryItemIds.Broccoli, "Брокколи", 16, GroceryCategoryIds.Products, 12),
            food(GroceryItemIds.Potato, "Картофель", 14, GroceryCategoryIds.Products, 16),
            food(GroceryItemIds.Corn, "Кукуруза", 18, GroceryCategoryIds.Products, 15),
            food(GroceryItemIds.Bread, "Хлеб", 20, GroceryCategoryIds.Products, 22),
            food(GroceryItemIds.Cheese, "Сыр", 28, GroceryCategoryIds.Products, 20, happiness = 1),
            food(GroceryItemIds.Egg, "Яйцо", 18, GroceryCategoryIds.Products, 18),
            food(GroceryItemIds.Yogurt, "Йогурт", 30, GroceryCategoryIds.Products, 20, happiness = 1),
        )

        private val MEALS = listOf(
            food(GroceryItemIds.Salad, "Салат", 35, GroceryCategoryIds.Meals, 23),
            food(GroceryItemIds.Soup, "Суп", 45, GroceryCategoryIds.Meals, 35),
            food(GroceryItemIds.Sandwich, "Сэндвич", 40, GroceryCategoryIds.Meals, 30),
            food(GroceryItemIds.FishWithLemon, "Рыба с лимоном", 60, GroceryCategoryIds.Meals, 42, happiness = 2),
            food(GroceryItemIds.Pizza, "Пицца", 55, GroceryCategoryIds.Meals, 40, happiness = 3),
            food(GroceryItemIds.Pasta, "Паста", 48, GroceryCategoryIds.Meals, 38, happiness = 2),
            food(GroceryItemIds.Omelet, "Омлет", 38, GroceryCategoryIds.Meals, 32),
            food(GroceryItemIds.Pancakes, "Блинчики", 50, GroceryCategoryIds.Meals, 34, happiness = 3),
            food(GroceryItemIds.Rolls, "Роллы", 65, GroceryCategoryIds.Meals, 38, happiness = 4),
            food(GroceryItemIds.Cake, "Пирожное", 50, GroceryCategoryIds.Meals, 28, happiness = 5),
        )

        private val DRINKS = listOf(
            food(GroceryItemIds.Milk, "Молоко", 25, GroceryCategoryIds.Drinks, 15),
            food(GroceryItemIds.OrangeJuice, "Апельсиновый сок", 22, GroceryCategoryIds.Drinks, 8, happiness = 1),
            food(GroceryItemIds.AppleJuice, "Яблочный сок", 20, GroceryCategoryIds.Drinks, 8, happiness = 1),
            food(GroceryItemIds.Lemonade, "Лимонад", 25, GroceryCategoryIds.Drinks, 5, happiness = 2),
            food(GroceryItemIds.BerrySmoothie, "Ягодный смузи", 35, GroceryCategoryIds.Drinks, 18, happiness = 2),
            food(GroceryItemIds.StrawberryCocktail, "Клубничный коктейль", 40, GroceryCategoryIds.Drinks, 20, happiness = 3),
            food(GroceryItemIds.Cocoa, "Какао", 30, GroceryCategoryIds.Drinks, 10, happiness = 2),
            food(GroceryItemIds.Tea, "Чай", 15, GroceryCategoryIds.Drinks, 2),
            food(GroceryItemIds.Compote, "Компот", 20, GroceryCategoryIds.Drinks, 6, happiness = 1),
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

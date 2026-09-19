package github.detrig.products

/** Effects of consuming one unit. Satiety is a delta, not an absolute pet state. */
data class FoodEffects(
    val satietyPercent: Int,
    val happinessPoints: Int = 0,
) {
    init {
        require(satietyPercent in 0..100) { "Satiety effect must be between 0 and 100" }
        require(happinessPoints in 0..100) { "Happiness effect must be between 0 and 100" }
        require(satietyPercent > 0 || happinessPoints > 0) {
            "Food must provide at least one effect"
        }
    }
}

data class FoodItem(
    override val id: ProductId,
    override val title: String,
    override val imageKey: ProductImageKey,
    override val priceRub: Long,
    override val categoryId: StoreCategoryId,
    override val description: String? = null,
    val effects: FoodEffects,
) : SellableItem {
    init {
        require(title.isNotBlank()) { "Food title must not be blank" }
        require(priceRub > 0L) { "Food price must be positive" }
        require(description == null || description.isNotBlank()) {
            "Food description must be either absent or non-blank"
        }
    }
}

package github.detrig.feature.shop.presentation

/** Immutable snapshot displayed after a successful shop transaction. */
internal data class ShopReceipt(
    val number: String,
    val storeTitle: String,
    val lines: List<ShopReceiptLine>,
) {
    init {
        require(number.length == RECEIPT_NUMBER_LENGTH && number.all(Char::isDigit)) {
            "Receipt number must contain six digits"
        }
        require(storeTitle.isNotBlank()) { "Receipt store title must not be blank" }
        require(lines.isNotEmpty()) { "Receipt must contain at least one line" }
    }

    val totalRub: Long = lines.fold(0L) { total, line -> Math.addExact(total, line.totalRub) }

    private companion object {
        const val RECEIPT_NUMBER_LENGTH = 6
    }
}

internal data class ShopReceiptLine(
    val title: String,
    val unitPriceRub: Long,
    val quantity: Int,
) {
    init {
        require(title.isNotBlank()) { "Receipt line title must not be blank" }
        require(unitPriceRub > 0L) { "Receipt unit price must be positive" }
        require(quantity > 0) { "Receipt quantity must be positive" }
    }

    val totalRub: Long = Math.multiplyExact(unitPriceRub, quantity.toLong())
}

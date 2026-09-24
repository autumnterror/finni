package github.detrig.feature.shop.presentation

import github.detrig.feature.shop.api.ShopPurchaseFeedback

/** Immutable snapshot displayed after a successful shop transaction. */
internal data class ShopReceipt(
    val number: String,
    val storeTitle: String,
    val lines: List<ShopReceiptLine>,
    val feedback: ShopPurchaseFeedback? = null,
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
    val totalRub: Long = Math.multiplyExact(unitPriceRub, quantity.toLong()),
) {
    init {
        require(title.isNotBlank()) { "Receipt line title must not be blank" }
        require(unitPriceRub > 0L) { "Receipt unit price must be positive" }
        require(quantity > 0) { "Receipt quantity must be positive" }
        require(totalRub > 0L) { "Receipt line total must be positive" }
        require(totalRub <= Math.multiplyExact(unitPriceRub, quantity.toLong())) {
            "Receipt line total must not exceed the regular total"
        }
    }
}

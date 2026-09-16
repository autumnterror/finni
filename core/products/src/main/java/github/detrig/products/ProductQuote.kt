package github.detrig.products

/** Расчёт по текущему каталогу: не списывает деньги и не подтверждает покупку. */
data class ProductQuote(val lines: List<ProductQuoteLine>) {
    val totalRub: Long = lines.fold(0L) { total, line -> Math.addExact(total, line.totalRub) }
}

data class ProductQuoteLine(val product: Product, val quantity: Int) {
    init { require(quantity > 0) }

    val totalRub: Long = product.unitPriceRub.toLong() * quantity
}

package github.detrig.feature.productmarket.presentation

import androidx.annotation.StringRes
import github.detrig.feature.productmarket.R
import github.detrig.feature.productmarket.domain.MarketDepartment
import github.detrig.products.ProductKind

@StringRes
internal fun ProductKind.nameRes(): Int = when (this) {
    ProductKind.GROATS -> R.string.market_groats
    ProductKind.CARROT -> R.string.market_carrot
    ProductKind.APPLE -> R.string.market_apple
    ProductKind.BERRIES -> R.string.market_berries
    ProductKind.CRACKERS -> R.string.market_crackers
    ProductKind.MILK -> R.string.market_milk
    ProductKind.YOGURT -> R.string.market_yogurt
    ProductKind.READY_MEAL -> R.string.market_ready_meal
}
@StringRes
internal fun MarketDepartment.nameRes(): Int = when (this) {
    MarketDepartment.PRODUCE -> R.string.market_produce
    MarketDepartment.BREAKFAST -> R.string.market_breakfast
    MarketDepartment.FRESH -> R.string.market_fresh
    MarketDepartment.GROATS -> R.string.market_groats_department
    MarketDepartment.REMINDER -> R.string.market_reminder
    MarketDepartment.BEFORE_CHECKOUT -> R.string.market_before_checkout
}

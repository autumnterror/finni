package github.detrig.feature.productmarket.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import github.detrig.designsystem.component.FinPetCoinText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import github.detrig.designsystem.component.FinPetOutlinedButton
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.productmarket.R
import github.detrig.feature.productmarket.domain.MarketPhase
import github.detrig.feature.productmarket.domain.MarketTrip
import github.detrig.feature.productmarket.presentation.art.MarketVector
import github.detrig.feature.productmarket.presentation.art.productArtwork
import github.detrig.products.ProductCatalog
import github.detrig.products.ProductId
import github.detrig.products.ProductQuantity
import github.detrig.products.ProductQuote
import github.detrig.products.DefaultProductCatalog
import github.detrig.products.ProductIds

@Composable
internal fun MarketCartDialog(
    trip: MarketTrip, catalog: ProductCatalog, onRemove: (ProductId) -> Unit, onDismiss: () -> Unit,
) {
    val quote = catalog.quote(trip.cart.map { (id, quantity) -> ProductQuantity(id, quantity) })
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = AppTheme.shapes.dialog, border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.borderDefault)) {
            Column(Modifier.padding(AppTheme.spacing.lg).testTag("market_cart_dialog"),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                Text(stringResource(R.string.market_cart), style = AppTheme.typography.sectionTitle)
                CartItems(trip, quote, onRemove, Modifier.weight(1f, fill = false).heightIn(max = 340.dp))
                CartTotal(quote)
                FinPetOutlinedButton(stringResource(R.string.market_continue), onDismiss, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun MarketCheckout(
    state: MarketViewState, catalog: ProductCatalog, onEvent: (MarketViewEvent) -> Unit, modifier: Modifier,
) {
    val trip = requireNotNull(state.trip)
    val quote = catalog.quote(trip.cart.map { (id, quantity) -> ProductQuantity(id, quantity) })
    val finished = trip.phase == MarketPhase.FINISHED
    Surface(modifier.testTag("market_checkout"),
        shape = AppTheme.shapes.card,
        border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.borderDefault)) {
        Column(Modifier.padding(AppTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
            Text(stringResource(if (finished) R.string.market_finished else R.string.market_checkout),
                style = AppTheme.typography.sectionTitle)
            if (finished) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                    Text(stringResource(R.string.market_collected, trip.itemCount), style = AppTheme.typography.bodyStrong)
                    val lesson = when {
                        trip.missing().isNotEmpty() -> R.string.market_learning_missing
                        trip.cart.keys.any { trip.extra(it) > 0 } -> R.string.market_learning_extra
                        else -> R.string.market_learning_complete
                    }
                    Text(stringResource(lesson), style = AppTheme.typography.body)
                    Text(stringResource(R.string.market_paid), style = AppTheme.typography.caption)
                }
                CartTotal(quote)
                FinPetOutlinedButton(stringResource(R.string.market_new_trip), { onEvent(MarketViewEvent.NewTrip) },
                    Modifier.fillMaxWidth(), enabled = !state.busy)
            } else {
                MissingProducts(trip, catalog)
                CartItems(trip, quote, { onEvent(MarketViewEvent.Remove(it)) },
                    Modifier.weight(1f, fill = false), enabled = !state.busy)
                CartTotal(quote)
                state.paymentMissingRub?.let { missing ->
                    Text(if (missing > 0) stringResource(R.string.market_payment_missing, missing)
                        else stringResource(R.string.market_payment_error),
                        style = AppTheme.typography.caption, color = AppTheme.colors.statusCritical.accent)
                }
                if (state.goalSaved) {
                    Text(stringResource(R.string.market_goal_saved), style = AppTheme.typography.caption,
                        color = AppTheme.colors.statusPositive.accent)
                }
                FinPetOutlinedButton(stringResource(R.string.market_save_cart_goal),
                    { onEvent(MarketViewEvent.SaveCartAsGoal) }, Modifier.fillMaxWidth(),
                    enabled = !state.busy && trip.cart.isNotEmpty())
                FinPetOutlinedButton(stringResource(R.string.market_another_pass), { onEvent(MarketViewEvent.AnotherPass) },
                    Modifier.fillMaxWidth(), enabled = !state.busy)
                FinPetOutlinedButton(stringResource(R.string.market_pay, quote.totalRub), { onEvent(MarketViewEvent.Finish) },
                    Modifier.fillMaxWidth().testTag("market_finish"), enabled = !state.busy)
            }
        }
    }
}

@Composable
private fun CartTotal(quote: ProductQuote) {
    Text(stringResource(R.string.market_cart_total, quote.totalRub),
        Modifier.fillMaxWidth().testTag("market_cart_total"),
        style = AppTheme.typography.bodyStrong, textAlign = TextAlign.End)
}

@Preview(name = "Корзина магазина", widthDp = 360, heightDp = 450, showBackground = true)
@Composable
private fun MarketCartPreview() {
    FinPetTheme {
        MarketCheckout(
            state = MarketViewState(
                trip = MarketTrip(
                    id = "preview", routeVersion = 3,
                    requested = listOf(ProductQuantity(ProductIds.Carrot, 2)),
                    phase = MarketPhase.CHECKOUT,
                    cart = mapOf(ProductIds.Carrot to 2),
                ),
                balanceRub = 500,
                loading = false,
            ),
            catalog = DefaultProductCatalog(),
            onEvent = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MissingProducts(trip: MarketTrip, catalog: ProductCatalog) {
    val missing = trip.missing().map {
        stringResource(R.string.market_missing_item,
            stringResource(requireNotNull(catalog.find(it.productId)).kind.nameRes()), it.quantity)
    }.joinToString()
    Text(if (missing.isEmpty()) stringResource(R.string.market_all_collected)
        else stringResource(R.string.market_missing, missing),
        style = AppTheme.typography.caption)
}

@Composable
private fun CartItems(
    trip: MarketTrip, quote: ProductQuote, onRemove: (ProductId) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
) {
    if (trip.cart.isEmpty()) {
        Text(stringResource(R.string.market_cart_empty),
            modifier.padding(vertical = AppTheme.spacing.lg), style = AppTheme.typography.body)
    } else {
        LazyColumn(modifier.testTag("market_cart_items")) {
            items(quote.lines, key = { it.product.id.value }) { line ->
                val id = line.product.id
                val kind = line.product.kind
                val name = stringResource(kind.nameRes())
                Row(Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.preferredTouchTarget),
                    verticalAlignment = Alignment.CenterVertically) {
                    MarketVector(productArtwork(kind), Modifier.size(32.dp, 42.dp))
                    Column(Modifier.weight(1f).padding(horizontal = AppTheme.spacing.sm)) {
                        Text(name, style = AppTheme.typography.label)
                        Text(stringResource(R.string.market_cart_line_price, line.quantity, line.product.unitPriceRub, line.totalRub),
                            style = AppTheme.typography.caption)
                        if (trip.extra(id) > 0) Text(stringResource(R.string.market_extra, trip.extra(id)),
                            style = AppTheme.typography.caption)
                    }
                    val removeLabel = stringResource(R.string.market_remove, name)
                    TextButton(onClick = { onRemove(id) }, enabled = enabled,
                        modifier = Modifier.size(AppTheme.sizes.minimumTouchTarget)
                            .testTag("remove_${id.value}").semantics { contentDescription = removeLabel }) {
                        Text("−", style = AppTheme.typography.currency)
                    }
                }
            }
        }
    }
}

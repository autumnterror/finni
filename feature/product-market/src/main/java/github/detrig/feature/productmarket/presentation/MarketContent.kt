package github.detrig.feature.productmarket.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import github.detrig.designsystem.component.FinPetCoinText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetOutlinedButton
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.productmarket.R
import github.detrig.feature.productmarket.domain.MarketConfiguration
import github.detrig.feature.productmarket.domain.MarketTrip
import github.detrig.feature.productmarket.presentation.art.MarketVector
import github.detrig.feature.productmarket.presentation.art.productArtwork
import github.detrig.products.ProductCatalog
import github.detrig.products.DefaultProductCatalog

@Composable
internal fun MarketContent(
    state: MarketViewState,
    configuration: MarketConfiguration,
    catalog: ProductCatalog,
    pickup: MarketViewCommand.Pickup?,
    onEvent: (MarketViewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(AppTheme.colors.surfaceBase)
        .windowInsetsPadding(WindowInsets.safeDrawing).testTag("product_market")) {
        MarketHeader(state, configuration, catalog, onEvent)
        if (state.trip == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                if (state.loading) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.market_loading), style = AppTheme.typography.body)
                }
            }
        } else {
            MarketScene(state, configuration, catalog, pickup, onEvent, Modifier.weight(1f).fillMaxWidth())
        }
    }
    if (state.cartOpen && state.trip != null) {
        MarketCartDialog(state.trip, catalog,
            onRemove = { onEvent(MarketViewEvent.Remove(it)) },
            onDismiss = { onEvent(MarketViewEvent.CloseCart) })
    }
    if (state.exitConfirmationOpen && state.error == null) {
        AlertDialog(
            modifier = Modifier.testTag("market_exit_dialog"),
            onDismissRequest = { onEvent(MarketViewEvent.CancelExit) },
            title = { Text(stringResource(R.string.market_exit_title)) },
            text = { Text(stringResource(R.string.market_exit_message)) },
            confirmButton = {
                FinPetOutlinedButton(
                    stringResource(if (state.busy) R.string.market_saving else R.string.market_exit),
                    { onEvent(MarketViewEvent.ConfirmExit) },
                    Modifier.testTag("market_confirm_exit"), enabled = !state.busy,
                )
            },
            dismissButton = {
                TextButton(onClick = { onEvent(MarketViewEvent.CancelExit) }, enabled = !state.busy,
                    modifier = Modifier.testTag("market_cancel_exit")) {
                    Text(stringResource(R.string.market_stay))
                }
            },
        )
    }
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.market_error_title)) },
            text = { Text(stringResource(when (error) {
                MarketError.LOAD -> R.string.market_error_load
                MarketError.SAVE -> R.string.market_error_save
                MarketError.BALANCE -> R.string.market_error_balance
            })) },
            confirmButton = {
                FinPetOutlinedButton(stringResource(R.string.market_retry),
                    onClick = { onEvent(MarketViewEvent.Retry) }, enabled = !state.busy)
            },
            dismissButton = {
                TextButton(onClick = { onEvent(MarketViewEvent.LeaveAfterError) }, enabled = !state.busy) {
                    Text(stringResource(if (error == MarketError.SAVE) R.string.market_leave_unsaved
                        else R.string.market_leave))
                }
            },
        )
    }
}

@Preview(name = "Магазин", widthDp = 360, heightDp = 640)
@Composable
private fun MarketContentPreview() {
    val config = MarketConfiguration()
    FinPetTheme {
        MarketContent(
            state = MarketViewState(
                trip = MarketTrip(id = "preview", routeVersion = config.routeVersion, requested = config.requested),
                balanceRub = 500,
                loading = false,
                foreground = true,
            ),
            configuration = config,
            catalog = DefaultProductCatalog(),
            pickup = null,
            onEvent = {},
        )
    }
}

@Composable
private fun MarketHeader(
    state: MarketViewState, configuration: MarketConfiguration, catalog: ProductCatalog,
    onEvent: (MarketViewEvent) -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onEvent(MarketViewEvent.Back) },
            modifier = Modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget).testTag("market_exit"),
            enabled = !state.busy && state.error == null) {
            Text(stringResource(R.string.market_exit), style = AppTheme.typography.label)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            (state.trip?.requested ?: configuration.requested).forEach { need ->
                val kind = requireNotNull(catalog.find(need.productId)).kind
                val name = stringResource(kind.nameRes())
                val count = state.trip?.cart?.get(need.productId) ?: 0
                val description = stringResource(R.string.market_list_description, name, count, need.quantity)
                Column(Modifier.weight(1f).padding(vertical = AppTheme.spacing.xs)
                    .testTag("list_${need.productId.value}")
                    .clearAndSetSemantics { contentDescription = description }, horizontalAlignment = Alignment.CenterHorizontally) {
                    MarketVector(productArtwork(kind), Modifier.size(34.dp, 42.dp))
                    Text(stringResource(R.string.market_list_count, count.coerceAtMost(need.quantity), need.quantity) +
                        if (count >= need.quantity) " ✓" else "", style = AppTheme.typography.caption, maxLines = 1)
                }
            }
        }
        val balance = state.balanceRub
        val description = if (balance == null) stringResource(R.string.market_loading)
            else stringResource(R.string.market_balance_description, balance)
        Text(if (balance == null) "…" else stringResource(R.string.market_balance, balance),
            Modifier.widthIn(min = 78.dp, max = 116.dp).padding(start = AppTheme.spacing.sm)
                .testTag("market_balance").clearAndSetSemantics { contentDescription = description },
            style = AppTheme.typography.bodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

package github.detrig.feature.shop.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetIconButton
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.shop.R
import kotlin.math.min

private val ReceiptQuantityColumnWidth = 76.dp
private val ReceiptAmountColumnWidth = 84.dp

/** Receipt shown after the economy transaction has been completed. */
@Composable
internal fun ShopReceiptDialog(
    receipt: ShopReceipt,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            FinPetCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = AppTheme.sizes.contentMaxWidth),
                shape = AppTheme.shapes.dialog,
                containerColor = AppTheme.colors.storefront.surface,
                borderColor = AppTheme.colors.storefront.outline,
                borderWidth = AppTheme.sizes.borderStrong,
                elevation = AppTheme.elevation.high,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 640.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(top = AppTheme.spacing.lg),
                ) {
                    ShopReceiptHeading(receipt = receipt, onDismiss = onDismiss)
                    ShopReceiptDivider(modifier = Modifier.padding(horizontal = AppTheme.spacing.lg))
                    ShopReceiptTableHeader()
                    receipt.lines.forEach { line -> ShopReceiptLineItem(line) }
                    ShopReceiptDivider(modifier = Modifier.padding(horizontal = AppTheme.spacing.lg))
                    ShopReceiptTotal(totalRub = receipt.totalRub)
                    ShopReceiptTearEdge()
                }
            }
        }
    }
}

@Composable
private fun ShopReceiptHeading(
    receipt: ShopReceipt,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.lg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.sizes.minimumTouchTarget),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Text(
                text = receipt.storeTitle.uppercase(),
                style = AppTheme.typography.brand,
                color = AppTheme.colors.storefront.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.shop_receipt_number, receipt.number),
                style = AppTheme.typography.sectionTitle,
                color = AppTheme.colors.textSecondary,
            )
        }
        FinPetIconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            ShopReceiptCloseIcon()
        }
    }
}

@Composable
private fun ShopReceiptTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.spacing.lg,
                vertical = AppTheme.spacing.md,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(R.string.shop_receipt_item),
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.sectionTitle,
            color = AppTheme.colors.storefront.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.shop_receipt_quantity),
            modifier = Modifier.width(ReceiptQuantityColumnWidth),
            style = AppTheme.typography.sectionTitle,
            color = AppTheme.colors.storefront.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        Text(
            text = stringResource(R.string.shop_receipt_amount),
            modifier = Modifier.width(ReceiptAmountColumnWidth),
            style = AppTheme.typography.sectionTitle,
            color = AppTheme.colors.storefront.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun ShopReceiptLineItem(line: ShopReceiptLine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.spacing.lg,
                vertical = AppTheme.spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = line.title,
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.screenTitle,
            color = AppTheme.colors.storefront.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = line.quantity.toString(),
            modifier = Modifier.width(ReceiptQuantityColumnWidth),
            style = AppTheme.typography.screenTitle,
            color = AppTheme.colors.storefront.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = line.totalRub.toString(),
            modifier = Modifier.width(ReceiptAmountColumnWidth),
            style = AppTheme.typography.screenTitle,
            color = AppTheme.colors.storefront.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

@Composable
private fun ShopReceiptTotal(totalRub: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.spacing.lg,
                vertical = AppTheme.spacing.xl,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.shop_receipt_total),
            style = AppTheme.typography.brand,
            color = AppTheme.colors.storefront.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        ShopCurrencyAmount(
            amountRub = totalRub,
            style = AppTheme.typography.brand,
            coinSize = 36.dp,
        )
    }
}

@Composable
private fun ShopReceiptDivider(modifier: Modifier = Modifier) {
    val color = AppTheme.colors.currencyAccent
    val strokeWidth = AppTheme.sizes.borderStrong
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
    ) {
        drawLine(
            color = color,
            start = Offset.Zero,
            end = Offset(size.width, 0f),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 8.dp.toPx())),
        )
    }
}

@Composable
private fun ShopReceiptTearEdge() {
    val color = AppTheme.colors.storefront.outline
    val strokeWidth = AppTheme.sizes.borderStrong
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        val toothWidth = 24.dp.toPx()
        var startX = 0f
        while (startX < size.width) {
            val middleX = min(startX + toothWidth / 2f, size.width)
            val endX = min(startX + toothWidth, size.width)
            drawLine(
                color = color,
                start = Offset(startX, 0f),
                end = Offset(middleX, size.height),
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(middleX, size.height),
                end = Offset(endX, 0f),
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round,
            )
            startX += toothWidth
        }
    }
}

@Composable
private fun ShopReceiptCloseIcon() {
    val iconSize = AppTheme.sizes.iconMedium
    val color = AppTheme.colors.storefront.onSurface
    val strokeWidth = AppTheme.sizes.borderStrong * 1.5f
    Canvas(modifier = Modifier.size(iconSize)) {
        val inset = size.minDimension * 0.22f
        drawLine(
            color = color,
            start = Offset(inset, inset),
            end = Offset(size.width - inset, size.height - inset),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width - inset, inset),
            end = Offset(inset, size.height - inset),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Preview(name = "Shop receipt", widthDp = 432, heightDp = 920, showBackground = true)
@Composable
private fun ShopReceiptDialogPreview() {
    FinPetTheme {
        ShopReceiptDialog(
            receipt = ShopReceipt(
                number = "010001",
                storeTitle = "Продуктовый",
                lines = listOf(
                    ShopReceiptLine("Яблоко", unitPriceRub = 15, quantity = 2),
                    ShopReceiptLine("Салат", unitPriceRub = 35, quantity = 1),
                    ShopReceiptLine("Молоко", unitPriceRub = 25, quantity = 1),
                ),
            ),
            onDismiss = {},
        )
    }
}

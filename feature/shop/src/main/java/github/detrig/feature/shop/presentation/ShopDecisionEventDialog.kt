package github.detrig.feature.shop.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.FinPetModalVisibilityEffect
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.shop.R
import github.detrig.feature.shop.api.ShopPetPortrait
import github.detrig.feature.shop.domain.ShopDecisionEvent
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.feature.shop.domain.ShopPromotionKind
import github.detrig.products.GroceryCatalog
import github.detrig.products.GroceryStoreIds
import androidx.compose.ui.res.stringResource

@Composable
internal fun ShopDecisionEventDialog(
    event: ShopDecisionEvent,
    speakerName: String,
    portrait: ShopPetPortrait,
    onFinished: () -> Unit,
) {
    FinPetModalVisibilityEffect()
    val text = when (event.type) {
        ShopDecisionEventType.PROMOTION -> when (event.promotionKind) {
            ShopPromotionKind.PERCENT_DISCOUNT -> stringResource(
                R.string.shop_promotion_prompt,
                event.productTitle,
                event.regularPriceRub,
                event.offeredPriceRub,
            )
            ShopPromotionKind.BUY_TWO_GET_ONE_FREE -> stringResource(
                R.string.shop_promotion_two_plus_one_prompt,
                event.productTitle,
            )
        }
        ShopDecisionEventType.IMPULSE_WISH -> stringResource(
            R.string.shop_impulse_prompt,
            event.productTitle,
            event.offeredPriceRub,
        )
    }
    FinPetDialogueDialog(
        speakerName = speakerName,
        cards = listOf(text),
        portrait = portrait::Content,
        onFinished = onFinished,
    )
}

@Preview(name = "Shop learning event", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun ShopDecisionEventDialogPreview() {
    val item = GroceryCatalog().storefront.items.first()
    FinPetTheme {
        ShopDecisionEventDialog(
            event = ShopDecisionEvent(
                eventId = "preview-promotion",
                type = ShopDecisionEventType.PROMOTION,
                gamePeriod = 1,
                eventPeriod = 1,
                storeId = GroceryStoreIds.Store,
                productId = item.id,
                productTitle = item.title,
                regularPriceRub = item.priceRub,
                offeredPriceRub = (item.priceRub - 1).coerceAtLeast(1),
            ),
            speakerName = "Пончик",
            portrait = ShopPetPortrait { Text("🐹") },
            onFinished = {},
        )
    }
}

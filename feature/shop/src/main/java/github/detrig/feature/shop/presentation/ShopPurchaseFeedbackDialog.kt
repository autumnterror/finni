package github.detrig.feature.shop.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.FinPetModalVisibilityEffect
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.shop.R
import github.detrig.feature.shop.api.ShopPetPortrait
import github.detrig.feature.shop.api.ShopPurchaseFeedback

@Composable
internal fun ShopPurchaseFeedbackDialog(
    feedback: ShopPurchaseFeedback,
    speakerName: String,
    portrait: ShopPetPortrait,
    onFinished: () -> Unit,
) {
    FinPetModalVisibilityEffect()
    val message = when (feedback) {
        ShopPurchaseFeedback.REQUIRED_FOOD_MISSING ->
            stringResource(R.string.shop_feedback_food_missing)
        ShopPurchaseFeedback.MANDATORY_MONEY_AT_RISK ->
            stringResource(R.string.shop_feedback_mandatory_money)
        ShopPurchaseFeedback.PROMOTION_OVERBUY ->
            stringResource(R.string.shop_feedback_promotion_overbuy)
        ShopPurchaseFeedback.TOO_MANY_EXTRAS ->
            stringResource(R.string.shop_feedback_too_many_extras)
    }
    FinPetDialogueDialog(
        speakerName = speakerName,
        cards = listOf(message),
        portrait = portrait::Content,
        onFinished = onFinished,
    )
}

@Preview(name = "Purchase feedback", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun ShopPurchaseFeedbackDialogPreview() {
    FinPetTheme {
        ShopPurchaseFeedbackDialog(
            feedback = ShopPurchaseFeedback.PROMOTION_OVERBUY,
            speakerName = "Пончик",
            portrait = ShopPetPortrait { Text("🐹") },
            onFinished = {},
        )
    }
}

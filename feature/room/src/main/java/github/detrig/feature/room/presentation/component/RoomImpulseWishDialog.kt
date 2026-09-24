package github.detrig.feature.room.presentation.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.FinPetModalVisibilityEffect
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.domain.model.RoomImpulseWish

@Composable
internal fun RoomImpulseWishDialog(
    wish: RoomImpulseWish,
    petName: String,
    petPortrait: @Composable (Modifier) -> Unit,
    onFinished: () -> Unit,
) {
    FinPetModalVisibilityEffect()
    val wishText = when (wish.phraseVariant) {
        0 -> stringResource(R.string.impulse_wish_variant_enough_money, wish.productTitle)
        1 -> stringResource(R.string.impulse_wish_variant_afford, wish.productTitle)
        2 -> stringResource(R.string.impulse_wish_variant_check_plan, wish.productTitle)
        else -> stringResource(R.string.impulse_wish_variant_money_after, wish.productTitle)
    }
    val cards = if (wish.showIntroduction) {
        listOf(
            stringResource(R.string.impulse_wish_intro_feeling),
            stringResource(R.string.impulse_wish_intro_check),
            wishText,
        )
    } else {
        listOf(wishText)
    }
    FinPetDialogueDialog(
        speakerName = petName,
        cards = cards,
        portrait = petPortrait,
        onFinished = onFinished,
    )
}

@Preview(name = "Impulse wish", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun RoomImpulseWishDialogPreview() {
    FinPetTheme {
        RoomImpulseWishDialog(
            wish = RoomImpulseWish(
                eventId = "preview-wish",
                productTitle = "ягодный йогурт",
                phraseVariant = 1,
                showIntroduction = true,
            ),
            petName = "Пончик",
            petPortrait = { Text("🐹") },
            onFinished = {},
        )
    }
}

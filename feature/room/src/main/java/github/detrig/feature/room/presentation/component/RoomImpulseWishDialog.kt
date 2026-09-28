package github.detrig.feature.room.presentation.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
    dismissOnBackPress: Boolean = true,
) {
    FinPetModalVisibilityEffect()
    val wishText = when (wish.kind) {
        RoomImpulseWish.Kind.FREE -> stringResource(R.string.impulse_wish_free, wish.productTitle)
        RoomImpulseWish.Kind.MINI_GAME -> stringResource(R.string.impulse_wish_play_game, wish.productTitle)
        RoomImpulseWish.Kind.SAVINGS_TOP_UP -> stringResource(R.string.impulse_wish_top_up_savings)
        else -> when (wish.phraseVariant) {
            0 -> stringResource(R.string.impulse_wish_variant_enough_money, wish.productTitle)
            1 -> stringResource(R.string.impulse_wish_variant_afford, wish.productTitle)
            2 -> stringResource(R.string.impulse_wish_variant_check_plan, wish.productTitle)
            else -> stringResource(R.string.impulse_wish_variant_money_after, wish.productTitle)
        }
    }
    val cards = if (wish.kind == RoomImpulseWish.Kind.FREE ||
        wish.kind == RoomImpulseWish.Kind.MINI_GAME ||
        wish.kind == RoomImpulseWish.Kind.SAVINGS_TOP_UP
    ) {
        listOf(wishText)
    } else if (wish.showIntroduction) {
        listOf(
            wishText,
            stringResource(R.string.impulse_wish_intro_feeling),
            stringResource(R.string.impulse_wish_intro_check),
            stringResource(R.string.impulse_wish_optional),
        )
    } else {
        listOf(wishText, stringResource(R.string.impulse_wish_optional))
    }
    FinPetDialogueDialog(
        speakerName = petName,
        cards = cards,
        portrait = petPortrait,
        dismissOnBackPress = dismissOnBackPress,
        onFinished = onFinished,
    )
}

@Preview(name = "Impulse wish", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun RoomImpulseWishDialogPreview(
    @PreviewParameter(RoomImpulseWishPreviewProvider::class) wish: RoomImpulseWish,
) {
    FinPetTheme {
        RoomImpulseWishDialog(
            wish = wish,
            petName = "Пончик",
            petPortrait = { Text("🐹") },
            onFinished = {},
        )
    }
}

private class RoomImpulseWishPreviewProvider : PreviewParameterProvider<RoomImpulseWish> {
    override val values = sequenceOf(
        RoomImpulseWish("preview-food", "Пирожное", 1, true),
        RoomImpulseWish("preview-clothes", "Синяя кепка", 1, false,
            kind = RoomImpulseWish.Kind.CLOTHING),
        RoomImpulseWish("preview-interior", "Уютный диван", 2, false,
            kind = RoomImpulseWish.Kind.TOY),
        RoomImpulseWish("preview-game", "Полёт", 0, false,
            kind = RoomImpulseWish.Kind.MINI_GAME),
        RoomImpulseWish("preview-savings", "Пополнить копилку", 0, false,
            kind = RoomImpulseWish.Kind.SAVINGS_TOP_UP),
    )
}

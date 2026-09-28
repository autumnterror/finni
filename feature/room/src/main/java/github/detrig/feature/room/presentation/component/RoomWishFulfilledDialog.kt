package github.detrig.feature.room.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetCoinText
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.FinPetModalVisibilityEffect
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.domain.model.RoomWishFulfillment

@Composable
internal fun RoomWishFulfilledDialog(
    fulfillment: RoomWishFulfillment,
    petName: String,
    petPortrait: @Composable (Modifier) -> Unit,
    onFinished: () -> Unit,
) {
    FinPetModalVisibilityEffect()
    val text = when (fulfillment.kind) {
        RoomWishFulfillment.Kind.ITEM -> if (fulfillment.title.isBlank()) {
            stringResource(R.string.wish_fulfilled_item)
        } else stringResource(R.string.wish_fulfilled_named_item, fulfillment.title)
        RoomWishFulfillment.Kind.MINI_GAME -> stringResource(R.string.wish_fulfilled_game)
        RoomWishFulfillment.Kind.SAVINGS_TOP_UP -> stringResource(R.string.wish_fulfilled_top_up)
        RoomWishFulfillment.Kind.SAVINGS_HALF_WAY -> stringResource(R.string.wish_fulfilled_half_way)
        RoomWishFulfillment.Kind.SAVINGS_REACHED -> stringResource(R.string.wish_fulfilled_goal)
        RoomWishFulfillment.Kind.GAME_UNLOCKED -> stringResource(R.string.wish_fulfilled_unlocked_game)
    }
    FinPetDialogueDialog(
        speakerName = petName,
        cards = listOf(text),
        portrait = petPortrait,
        onFinished = onFinished,
    )
}

@Preview(name = "Желание выполнено", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun RoomWishFulfilledDialogPreview() {
    FinPetTheme {
        RoomWishFulfilledDialog(
            fulfillment = RoomWishFulfillment("preview", RoomWishFulfillment.Kind.ITEM, "Пирожное"),
            petName = "Финни",
            petPortrait = { FinPetCoinText("😸") },
            onFinished = {},
        )
    }
}

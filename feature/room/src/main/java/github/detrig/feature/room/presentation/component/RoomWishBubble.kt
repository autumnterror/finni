package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.api.RoomWishArtwork
import github.detrig.feature.room.domain.model.RoomImpulseWish

@Composable
internal fun RoomWishBubble(
    wish: RoomImpulseWish,
    artwork: RoomWishArtwork,
    modifier: Modifier = Modifier,
) {
    Box(modifier.semantics { contentDescription = "Желание питомца: ${wish.productTitle}" }) {
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .size(12.dp)
                .background(AppTheme.colors.surfaceElevated, CircleShape),
        )
        FinPetCard(
            modifier = Modifier.fillMaxSize().padding(start = 6.dp, bottom = 6.dp),
            shape = AppTheme.shapes.card,
            containerColor = AppTheme.colors.surfaceElevated,
            borderColor = AppTheme.colors.borderDefault,
        ) {
            Box(contentAlignment = Alignment.Center) {
                artwork.Content(wish, Modifier.fillMaxSize().padding(AppTheme.spacing.xs))
            }
        }
    }
}

@Preview(name = "Желание питомца", widthDp = 120, heightDp = 120)
@Composable
private fun RoomWishBubblePreview() {
    FinPetTheme {
        RoomWishBubble(
            wish = RoomImpulseWish("preview", "Пирожное", 0, false),
            artwork = RoomWishArtwork { _, modifier ->
                Box(modifier, contentAlignment = Alignment.Center) { Text("🍰") }
            },
            modifier = Modifier.size(84.dp),
        )
    }
}

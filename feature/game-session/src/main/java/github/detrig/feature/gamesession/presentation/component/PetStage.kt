package github.detrig.feature.gamesession.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.gamesession.R

@Composable
internal fun PetStage() {
    FinPetCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 236.dp),
        containerColor = AppTheme.colors.sceneBackground,
        borderColor = AppTheme.colors.sceneBorder,
        borderWidth = AppTheme.sizes.borderStrong,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(74.dp)
                    .background(AppTheme.colors.sceneGround),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .width(128.dp)
                    .height(18.dp)
                    .clip(AppTheme.shapes.badge)
                    .background(AppTheme.colors.sceneShadow),
            )
            PixelImage(
                drawableRes = R.drawable.ic_finpet_pet_smile,
                contentDescription = "Питомец",
                modifier = Modifier.align(Alignment.Center),
                size = 128.dp,
            )
        }
    }
}

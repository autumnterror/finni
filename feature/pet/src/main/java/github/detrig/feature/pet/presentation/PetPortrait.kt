package github.detrig.feature.pet.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.model.GrowthStage

/** Цветной крупный план мордочки для диалогов и будущих экранов обучения. */
@Composable
fun PetPortrait(
    profile: PetProfile,
    modifier: Modifier = Modifier,
    growthStage: GrowthStage = GrowthStage.BABY,
    dirtStage: Int = 0,
) {
    val assets = rememberHamsterAssets()
    if (assets != null) {
        HamsterPreview(
            assets = assets,
            appearance = profile.hamsterAppearance,
            stage = growthStage,
            modifier = modifier,
            clothingLayers = rememberClothingLayers(profile.clothing.equippedBySlot, profile.hamsterAppearance),
            dirtStage = dirtStage,
        )
    }
}

@Preview(name = "Портрет питомца", widthDp = 220, heightDp = 220)
@Composable
private fun PetPortraitPreview() {
    FinPetTheme {
        PetPortrait(PetProfile(name = "Финни", color = PetColor.Sunny), Modifier.size(220.dp))
    }
}

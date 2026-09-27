package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetBackButton
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.api.RoomPetInteraction
import github.detrig.feature.room.domain.furniture.FurnitureVariant
import github.detrig.feature.room.presentation.BathStep

@Composable
internal fun BathroomScreen(
    washStep: BathStep,
    equipped: Map<String, FurnitureVariant>,
    petContent: @Composable (Modifier, RoomPetInteraction) -> Unit,
    onBack: () -> Unit,
    onToolCompleted: (BathStep) -> Unit,
    onToolSoundChanged: (BathStep, Boolean) -> Unit = { _, _ -> },
    onDryerRunningChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(AppTheme.colors.house.floor).testTag("bathroom_washing")) {
        BathroomScene(
            closeUp = true,
            equipped = equipped,
            modifier = Modifier.fillMaxSize(),
            petContent = petContent,
            washStep = washStep,
            onToolCompleted = onToolCompleted,
            onToolSoundChanged = onToolSoundChanged,
            onDryerRunningChanged = onDryerRunningChanged,
        )
        FinPetBackButton(
            onClick = onBack,
            contentDescription = "Назад в комнату",
            modifier = Modifier.align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(AppTheme.spacing.md),
        )
    }
}

@Preview(name = "Мытьё питомца", widthDp = 390, heightDp = 740)
@Composable
private fun BathroomScreenPreview() {
    FinPetTheme {
        BathroomScreen(
            washStep = BathStep.SOAP,
            equipped = emptyMap(),
            petContent = { _, _ -> },
            onBack = {},
            onToolCompleted = {},
        )
    }
}

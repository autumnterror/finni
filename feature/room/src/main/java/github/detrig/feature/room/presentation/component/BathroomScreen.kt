package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetBackButton
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetStorefrontCard
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
    onRestart: () -> Unit,
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
        )
        FinPetBackButton(
            onClick = onBack,
            contentDescription = "Назад в комнату",
            modifier = Modifier.align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(AppTheme.spacing.md),
        )
        FinPetStorefrontCard(
            modifier = Modifier.align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(AppTheme.spacing.md),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(AppTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                Text(
                    text = when (washStep) {
                        BathStep.SOAP -> "Возьми мыло с полки и поводи им по хомяку"
                        BathStep.RINSE -> "Возьми лейку и смой мыло водой"
                        BathStep.DRY -> "Возьми полотенце и вытри хомяка"
                        BathStep.CLEAN -> "Хомяк чистый!"
                    },
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.storefront.onSurface,
                )
                if (washStep == BathStep.CLEAN) {
                    FinPetButton(
                        text = "Помыть ещё раз",
                        onClick = onRestart,
                        modifier = Modifier.fillMaxWidth(),
                        style = FinPetButtonDefaults.storefrontPrimaryStyle(),
                    )
                }
            }
        }
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
            onRestart = {},
        )
    }
}

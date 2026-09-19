package github.detrig.feature.pet.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import github.detrig.feature.pet.domain.model.PetProfile
import kotlinx.coroutines.flow.Flow

interface PetApi {
    fun observeProfile(): Flow<PetProfile?>

    /** Не пропускает игрока в основной интерфейс, пока питомец не создан. */
    @Composable
    fun RequirePet(
        modifier: Modifier = Modifier,
        content: @Composable (PetProfile, onPetClick: () -> Unit, onCustomizeClick: () -> Unit) -> Unit,
    )

    /** Рисует актуальную внешность питомца в переданном игровой сценой месте. */
    @Composable
    fun Content(
        profile: PetProfile,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
    )

    /** Крупный план мордочки с выбранным цветом для общих карточек диалога. */
    @Composable
    fun Portrait(profile: PetProfile, modifier: Modifier = Modifier)

    /** Собирает актуальную внешность питомца для Canvas-сцен мини-игр. */
    @Composable
    fun rememberCurrentAppearanceBitmap(maxSidePx: Int): ImageBitmap?
}

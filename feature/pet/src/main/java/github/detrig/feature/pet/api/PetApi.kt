package github.detrig.feature.pet.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import github.detrig.feature.pet.domain.model.PetProfile
import kotlinx.coroutines.flow.Flow

interface PetApi {
    /** Blocks until the shared hamster assets are ready for the first frame. */
    suspend fun preloadAssets()

    fun observeProfile(): Flow<PetProfile?>
    fun currentProfile(): PetProfile?

    suspend fun clothingItems(): List<ClothingItem>
    fun cachedClothingItems(): List<ClothingItem>
    fun recordClothingPurchase(itemId: String)
    fun equipClothing(slot: String, itemId: String?)

    @Composable
    fun ClothingThumbnail(itemId: String, modifier: Modifier = Modifier)

    @Composable
    fun OutfitPreview(profile: PetProfile, equippedBySlot: Map<String, String>, modifier: Modifier = Modifier)

    /** Не пропускает игрока в основной интерфейс, пока питомец не создан. */
    @Composable
    fun RequirePet(
        modifier: Modifier = Modifier,
        content: @Composable (
            PetProfile,
            onPetClick: () -> Unit,
            onCustomizeClick: () -> Unit,
            isPetDialogueVisible: Boolean,
        ) -> Unit,
    )

    /** Рисует актуальную внешность питомца в переданном игровой сценой месте. */
    @Composable
    fun Content(
        profile: PetProfile,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        animateIdle: Boolean = true,
        mouthOpen: Boolean = false,
        lookAt: Offset? = null,
        pose: PetPose = PetPose.IDLE,
        gestureCallbacks: PetGestureCallbacks? = null,
        showShadow: Boolean = true,
    )

    /** Крупный план мордочки с выбранным цветом для общих карточек диалога. */
    @Composable
    fun Portrait(profile: PetProfile, modifier: Modifier = Modifier)

    /** Собирает актуальную внешность питомца для Canvas-сцен мини-игр. */
    @Composable
    fun rememberCurrentAppearanceBitmap(maxSidePx: Int): ImageBitmap?
}

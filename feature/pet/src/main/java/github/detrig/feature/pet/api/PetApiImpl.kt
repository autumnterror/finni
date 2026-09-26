package github.detrig.feature.pet.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.repository.PetRepository
import github.detrig.feature.pet.presentation.HamsterAssetsCache
import github.detrig.feature.pet.presentation.PetHostScreen
import github.detrig.feature.pet.presentation.PetScene
import github.detrig.feature.pet.presentation.PetPortrait
import github.detrig.feature.pet.presentation.ClothingArtwork
import github.detrig.feature.pet.presentation.ClothingThumbnail as PetClothingThumbnail
import github.detrig.feature.pet.presentation.HamsterPreview
import github.detrig.feature.pet.presentation.rememberHamsterAssets
import github.detrig.feature.pet.presentation.rememberClothingLayers
import github.detrig.feature.pet.presentation.rememberPetAppearanceBitmap

internal class PetApiImpl(
    private val repository: PetRepository,
    private val assets: android.content.res.AssetManager,
) : PetApi {
    override suspend fun preloadAssets() {
        HamsterAssetsCache.awaitPreloaded(assets)
        ClothingArtwork.preload(assets, repository.currentProfile())
    }

    override fun observeProfile() = repository.observeProfile()

    override fun currentProfile(): PetProfile? = repository.currentProfile()

    override suspend fun clothingItems(): List<ClothingItem> = ClothingArtwork.items(assets)

    override fun cachedClothingItems(): List<ClothingItem> = ClothingArtwork.cachedItems()

    override fun recordClothingPurchase(itemId: String) {
        repository.updateClothing { it.withPurchase(itemId) }
    }

    override fun equipClothing(slot: String, itemId: String?) {
        val profile = repository.updateClothing { it.withEquipped(slot, itemId) }
        ClothingArtwork.retainEquipped(profile)
    }

    @Composable
    override fun ClothingThumbnail(itemId: String, modifier: Modifier) {
        PetClothingThumbnail(itemId, modifier)
    }

    @Composable
    override fun OutfitPreview(profile: PetProfile, equippedBySlot: Map<String, String>, modifier: Modifier) {
        val hamsterAssets = rememberHamsterAssets()
        if (hamsterAssets != null) {
            HamsterPreview(
                assets = hamsterAssets,
                appearance = profile.hamsterAppearance,
                modifier = modifier,
                clothingLayers = rememberClothingLayers(equippedBySlot, profile.hamsterAppearance),
            )
        }
    }

    @Composable
    override fun RequirePet(
        modifier: Modifier,
        content: @Composable (PetProfile, () -> Unit, () -> Unit, Boolean) -> Unit,
    ) {
        PetHostScreen(modifier = modifier, content = content)
    }

    @Composable
    override fun Content(
        profile: PetProfile,
        modifier: Modifier,
        onClick: (() -> Unit)?,
        animateIdle: Boolean,
        mouthOpen: Boolean,
        lookAt: Offset?,
        pose: PetPose,
        gestureCallbacks: PetGestureCallbacks?,
        showShadow: Boolean,
    ) {
        PetScene(
            profile = profile,
            modifier = modifier,
            animateIdle = animateIdle,
            mouthOpen = mouthOpen,
            lookAt = lookAt,
            onClick = onClick,
            pose = pose,
            gestureCallbacks = gestureCallbacks,
            showShadow = showShadow,
        )
    }

    @Composable
    override fun Portrait(profile: PetProfile, modifier: Modifier) {
        PetPortrait(profile, modifier)
    }

    @Composable
    override fun rememberCurrentAppearanceBitmap(maxSidePx: Int): ImageBitmap? {
        val profile by repository.observeProfile().collectAsState(initial = null)
        return profile?.let { rememberPetAppearanceBitmap(it, maxSidePx) }
    }
}

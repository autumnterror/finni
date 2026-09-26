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
import github.detrig.feature.pet.presentation.rememberPetAppearanceBitmap

internal class PetApiImpl(
    private val repository: PetRepository,
    private val assets: android.content.res.AssetManager,
) : PetApi {
    override suspend fun preloadAssets() {
        HamsterAssetsCache.awaitPreloaded(assets)
    }

    override fun observeProfile() = repository.observeProfile()

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

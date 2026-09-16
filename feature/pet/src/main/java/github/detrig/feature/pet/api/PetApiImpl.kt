package github.detrig.feature.pet.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.repository.PetRepository
import github.detrig.feature.pet.presentation.PetHostScreen
import github.detrig.feature.pet.presentation.PetScene
import github.detrig.feature.pet.presentation.rememberPetAppearanceBitmap

internal class PetApiImpl(
    private val repository: PetRepository,
) : PetApi {
    override fun observeProfile() = repository.observeProfile()

    @Composable
    override fun RequirePet(
        modifier: Modifier,
        content: @Composable (PetProfile) -> Unit,
    ) {
        PetHostScreen(modifier = modifier, content = content)
    }

    @Composable
    override fun Content(profile: PetProfile, modifier: Modifier) {
        PetScene(profile = profile, modifier = modifier)
    }

    @Composable
    override fun rememberCurrentAppearanceBitmap(maxSidePx: Int): ImageBitmap? {
        val profile by repository.observeProfile().collectAsState(initial = null)
        return profile?.let { rememberPetAppearanceBitmap(it, maxSidePx) }
    }
}

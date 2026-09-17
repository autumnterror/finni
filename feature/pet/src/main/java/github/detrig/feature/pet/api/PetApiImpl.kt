package github.detrig.feature.pet.api

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import github.detrig.feature.pet.R
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.repository.PetRepository
import github.detrig.feature.pet.presentation.PetHostScreen
import github.detrig.feature.pet.presentation.PetScene
import github.detrig.feature.pet.presentation.PetPortrait
import github.detrig.feature.pet.presentation.rememberPetAppearanceBitmap

internal class PetApiImpl(
    private val repository: PetRepository,
) : PetApi {
    override fun observeProfile() = repository.observeProfile()

    @Composable
    override fun RequirePet(
        modifier: Modifier,
        content: @Composable (PetProfile, () -> Unit) -> Unit,
    ) {
        PetHostScreen(modifier = modifier, content = content)
    }

    @Composable
    override fun Content(profile: PetProfile, modifier: Modifier, onClick: (() -> Unit)?) {
        val clickModifier = if (onClick != null) {
            modifier.clickable(
                onClickLabel = stringResource(R.string.pet_talk_to, profile.name),
                role = Role.Button,
                onClick = onClick,
            )
        } else modifier
        PetScene(profile = profile, modifier = clickModifier)
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

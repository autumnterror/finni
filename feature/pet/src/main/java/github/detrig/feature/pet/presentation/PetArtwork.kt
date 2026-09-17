package github.detrig.feature.pet.presentation

import androidx.annotation.DrawableRes
import github.detrig.feature.pet.R
import github.detrig.feature.pet.domain.model.PetSpecies

internal data class PetFaceCrop(val left: Float, val top: Float, val side: Float)

internal data class PetArtwork(
    @DrawableRes val baseRes: Int,
    @DrawableRes val colorMaskRes: Int,
    val faceCrop: PetFaceCrop,
)

/** Портреты кадрируются из тех же базовых спрайтов и масок, что используются в комнате. */
internal fun PetSpecies.artwork(): PetArtwork = when (this) {
    PetSpecies.Cat -> PetArtwork(
        R.drawable.pet_cat_base, R.drawable.pet_cat_color_mask,
        PetFaceCrop(left = 0.12f, top = 0f, side = 0.76f),
    )
    PetSpecies.Dog -> PetArtwork(
        R.drawable.pet_dog_base, R.drawable.pet_dog_color_mask,
        PetFaceCrop(left = 0.08f, top = 0f, side = 0.84f),
    )
    PetSpecies.Rat -> PetArtwork(
        R.drawable.pet_rat_base, R.drawable.pet_rat_color_mask,
        PetFaceCrop(left = 0.12f, top = 0f, side = 0.76f),
    )
    PetSpecies.Rooster -> PetArtwork(
        R.drawable.pet_rooster_base, R.drawable.pet_rooster_color_mask,
        PetFaceCrop(left = 0.55f, top = 0f, side = 0.43f),
    )
}

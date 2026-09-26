package github.detrig.feature.pet.domain.model

data class PetProfile(
    val name: String,
    val species: PetSpecies = PetSpecies.Hamster,
    val color: PetColor,
    val hamsterAppearance: HamsterAppearance = HamsterAppearance(),
    val clothing: ClothingState = ClothingState(),
) {
    val appearanceId: String
        get() = "${species.storageKey}:${color.storageKey}"

    init {
        require(PetNameRules.validate(name) == null) { "Invalid pet name" }
        require(species == PetSpecies.Hamster) { "Only hamster profiles are supported" }
    }
}

package github.detrig.feature.pet.domain.model

data class PetProfile(
    val name: String,
    val species: PetSpecies,
    val color: PetColor,
) {
    val appearanceId: String
        get() = "${species.storageKey}:${color.storageKey}"

    init {
        require(PetNameRules.validate(name) == null) { "Invalid pet name" }
    }
}

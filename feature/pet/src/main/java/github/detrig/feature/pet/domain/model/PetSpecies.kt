package github.detrig.feature.pet.domain.model

enum class PetSpecies(val storageKey: String) {
    Cat("cat"),
    Dog("dog"),
    Rat("rat"),
    Rooster("rooster"),
    ;

    companion object {
        fun fromStorageKey(key: String): PetSpecies? = entries.find { it.storageKey == key }
    }
}

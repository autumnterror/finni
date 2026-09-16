package github.detrig.feature.pet.domain.model

enum class PetColor(val storageKey: String) {
    Sunny("sunny"),
    Mint("mint"),
    Coral("coral"),
    Sky("sky"),
    ;

    companion object {
        fun fromStorageKey(key: String): PetColor? = entries.find { it.storageKey == key }
    }
}

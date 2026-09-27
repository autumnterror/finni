package github.detrig.feature.pet.domain.model

/** Visual stage selected by the persisted game progression. Never use enum ordinals as asset IDs. */
enum class GrowthStage(val assetId: String) {
    BABY("baby"),
    TEEN("teen"),
    ADULT("adult"),

    ;

    companion object {
        fun fromAssetId(id: String?): GrowthStage? = when (id) {
            "baby" -> BABY
            "teen", "explorer" -> TEEN
            "adult", "companion" -> ADULT
            else -> null
        }
    }
}

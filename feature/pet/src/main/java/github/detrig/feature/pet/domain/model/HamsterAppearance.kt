package github.detrig.feature.pet.domain.model

/** Stable appearance ids from the bundled Finni hamster asset manifest. */
data class HamsterAppearance(
    val palette: String = DEFAULT_PALETTE,
    val coat: String = DEFAULT_COAT,
    val fur: String = DEFAULT_FUR,
    val ears: String = DEFAULT_EARS,
    val mark: String = DEFAULT_MARK,
    val eyes: String = DEFAULT_EYES,
) {
    fun properties(): Map<String, String> = mapOf(
        "palette" to palette,
        "coat" to coat,
        "fur" to fur,
        "ears" to ears,
        "mark" to mark,
        "eyes" to eyes,
    )

    companion object {
        const val DEFAULT_PALETTE = "caramel"
        const val DEFAULT_COAT = "belly"
        const val DEFAULT_FUR = "smooth"
        const val DEFAULT_EARS = "round"
        const val DEFAULT_MARK = "none"
        const val DEFAULT_EYES = "round"
    }
}

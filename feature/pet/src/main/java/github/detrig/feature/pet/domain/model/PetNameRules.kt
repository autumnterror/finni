package github.detrig.feature.pet.domain.model

object PetNameRules {
    const val MAX_LENGTH = 10

    fun normalize(value: String): String = value.trim().replace(WHITESPACE, " ")

    fun limit(value: String): String {
        val length = value.codePointCount(0, value.length)
        if (length <= MAX_LENGTH) return value
        return value.substring(0, value.offsetByCodePoints(0, MAX_LENGTH))
    }

    fun validate(value: String): PetNameValidationError? {
        val normalized = normalize(value)
        return when {
            normalized.isEmpty() -> PetNameValidationError.Empty
            normalized.codePointCount(0, normalized.length) > MAX_LENGTH -> PetNameValidationError.TooLong
            normalized.any { !it.isLetterOrDigit() && it != ' ' && it != '-' } ->
                PetNameValidationError.InvalidCharacters
            normalized.none(Char::isLetterOrDigit) -> PetNameValidationError.InvalidCharacters
            else -> null
        }
    }

    private val WHITESPACE = Regex("\\s+")
}

enum class PetNameValidationError {
    Empty,
    TooLong,
    InvalidCharacters,
}

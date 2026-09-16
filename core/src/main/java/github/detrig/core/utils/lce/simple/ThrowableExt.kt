package github.detrig.core.utils.lce.simple

/**
 * Получить текст ошибки для UI.
 */
fun Throwable?.getUiMessage(defaultMessage: String): String {
    val extractedError = (this as? SimpleLceException)?.cause ?: this
    return extractedError?.message?.takeIf { it.isNotBlank() } ?: defaultMessage
}

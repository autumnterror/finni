package github.detrig.core.utils.lce.simple

class SimpleLceException(
    override val message: String?,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleLceException) return false
        return message == other.message &&
            cause?.javaClass == other.cause?.javaClass &&
            cause?.message == other.cause?.message
    }

    override fun hashCode(): Int {
        var hashCode = message.hashCode()
        hashCode = hashCode * 31 + cause.hashCode()
        return hashCode
    }

    override fun toString(): String {
        return "SimpleLceException(message = $message, cause = $cause)"
    }
}

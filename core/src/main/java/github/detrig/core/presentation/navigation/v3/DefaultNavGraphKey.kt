package github.detrig.core.presentation.navigation.v3

import androidx.navigation3.runtime.NavKey

class DefaultNavGraphKey(
    private val name: String,
) : NavGraphKey {

    override fun toString(): String = "NavGraphKey-$name"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultNavGraphKey) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    companion object {
        operator fun invoke(startBackStack: List<NavKey>): NavGraphKey {
            val firstName = startBackStack.firstOrNull()?.let { route ->
                route::class.simpleName ?: route.toString()
            } ?: "Empty"

            return DefaultNavGraphKey(firstName)
        }
    }
}

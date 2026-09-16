package github.detrig.core.utils.lce.simple

import androidx.compose.runtime.Immutable
import github.detrig.core.mvvm.CoreViewState

/**
 * Готовый CoreViewState для простых экранов Loading/Content/Error.
 */
@Immutable
class SimpleLceViewState<out T : Any> internal constructor(
    private val stateLce: SimpleLce<T>,
) : SimpleLce<T> by stateLce, CoreViewState {

    companion object {
        fun <T : Any> loading(): SimpleLceViewState<T> = SimpleLceViewState(SimpleLce.loading())

        fun <T : Any> content(value: T): SimpleLceViewState<T> = SimpleLceViewState(SimpleLce.content(value))

        fun <T : Any> error(error: Throwable): SimpleLceViewState<T> = SimpleLceViewState(SimpleLce.error(error))

        fun <T : Any> error(): SimpleLceViewState<T> = SimpleLceViewState(SimpleLce.error())

        fun <T : Any> fromLce(stateLce: SimpleLce<T>): SimpleLceViewState<T> = SimpleLceViewState(stateLce)
    }

    override fun <R : Any> mapContent(transform: (T) -> R): SimpleLceViewState<R> {
        return SimpleLceViewState(stateLce.mapContent(transform))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleLceViewState<*>) return false
        return stateLce == other.stateLce
    }

    override fun hashCode(): Int {
        return stateLce.hashCode()
    }

    override fun toString(): String {
        return "SimpleLceViewState(stateLce = $stateLce)"
    }
}

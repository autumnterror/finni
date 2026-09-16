package github.detrig.core.utils.lce.simple

fun <T : Any> Result<T>.toLce(): SimpleLce<T> {
    return fold(
        onSuccess = { SimpleLce.content(it) },
        onFailure = { SimpleLce.error(it) },
    )
}

package github.detrig.core.presentation.message

import androidx.compose.runtime.Immutable

@Immutable
data class GlobalMessage(
    val id: Long,
    val text: String,
    val type: GlobalMessageType,
    val durationMillis: Long = DEFAULT_DURATION_MILLIS,
) {
    companion object {
        const val DEFAULT_DURATION_MILLIS = 3_500L
    }
}

enum class GlobalMessageType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
}

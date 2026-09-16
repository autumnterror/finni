package github.detrig.core.presentation.message.impl

import github.detrig.core.presentation.message.GlobalMessage
import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.core.presentation.message.GlobalMessageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

internal class GlobalMessageControllerImpl : GlobalMessageController {

    private val nextId = AtomicLong(0L)
    private val mutableMessages = MutableStateFlow<List<GlobalMessage>>(emptyList())

    override val messages: StateFlow<List<GlobalMessage>> = mutableMessages.asStateFlow()

    override fun showMessage(text: String) {
        show(text = text, type = GlobalMessageType.INFO)
    }

    override fun showSuccessMessage(text: String) {
        show(text = text, type = GlobalMessageType.SUCCESS)
    }

    override fun showWarningMessage(text: String) {
        show(text = text, type = GlobalMessageType.WARNING)
    }

    override fun showErrorMessage(text: String) {
        show(text = text, type = GlobalMessageType.ERROR)
    }

    override fun dismissMessage(id: Long) {
        mutableMessages.update { messages ->
            messages.filterNot { message -> message.id == id }
        }
    }

    private fun show(
        text: String,
        type: GlobalMessageType,
    ) {
        if (text.isBlank()) return

        val message = GlobalMessage(
            id = nextId.incrementAndGet(),
            text = text,
            type = type,
        )
        mutableMessages.update { messages ->
            (messages + message).takeLast(MAX_VISIBLE_MESSAGES)
        }
    }

    private companion object {
        const val MAX_VISIBLE_MESSAGES = 3
    }
}

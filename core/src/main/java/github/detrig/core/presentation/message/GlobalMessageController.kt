package github.detrig.core.presentation.message

import kotlinx.coroutines.flow.StateFlow

interface GlobalMessageController {

    val messages: StateFlow<List<GlobalMessage>>

    fun showMessage(text: String)

    fun showSuccessMessage(text: String)

    fun showWarningMessage(text: String)

    fun showErrorMessage(text: String)

    fun dismissMessage(id: Long)
}

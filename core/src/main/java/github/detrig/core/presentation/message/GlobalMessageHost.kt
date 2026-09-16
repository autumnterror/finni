package github.detrig.core.presentation.message

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@Composable
fun GlobalMessageHost(
    controller: GlobalMessageController,
    modifier: Modifier = Modifier,
) {
    val messages by controller.messages.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .zIndex(MESSAGE_HOST_Z_INDEX),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            messages.forEach { message ->
                GlobalMessageItem(
                    message = message,
                    onDismiss = { controller.dismissMessage(message.id) },
                )
            }
        }
    }
}

@Composable
private fun GlobalMessageItem(
    message: GlobalMessage,
    onDismiss: () -> Unit,
) {
    val colors = message.type.colors()

    LaunchedEffect(message.id) {
        delay(message.durationMillis)
        onDismiss()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = colors.container,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colors.accent, CircleShape),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = message.type.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.title,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text,
                )
            }
            TextButton(onClick = onDismiss) {
                Text(
                    text = "OK",
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private val GlobalMessageType.title: String
    get() = when (this) {
        GlobalMessageType.INFO -> "Сообщение"
        GlobalMessageType.SUCCESS -> "Готово"
        GlobalMessageType.WARNING -> "Внимание"
        GlobalMessageType.ERROR -> "Ошибка"
    }

private fun GlobalMessageType.colors(): GlobalMessageColors {
    return when (this) {
        GlobalMessageType.INFO -> GlobalMessageColors(
            container = Color(0xFFEFF6FF),
            border = Color(0xFFB8D8FF),
            accent = Color(0xFF1D69B3),
            title = Color(0xFF143B63),
            text = Color(0xFF1E3247),
        )
        GlobalMessageType.SUCCESS -> GlobalMessageColors(
            container = Color(0xFFEAF7EF),
            border = Color(0xFFB8DEC5),
            accent = Color(0xFF2F7D57),
            title = Color(0xFF1D4E36),
            text = Color(0xFF263A2E),
        )
        GlobalMessageType.WARNING -> GlobalMessageColors(
            container = Color(0xFFFFF5DA),
            border = Color(0xFFE6C978),
            accent = Color(0xFFB77A13),
            title = Color(0xFF6C480D),
            text = Color(0xFF4B3A1D),
        )
        GlobalMessageType.ERROR -> GlobalMessageColors(
            container = Color(0xFFFFECEC),
            border = Color(0xFFF0B6B6),
            accent = Color(0xFFC7363E),
            title = Color(0xFF7A1E24),
            text = Color(0xFF4C282B),
        )
    }
}

private data class GlobalMessageColors(
    val container: Color,
    val border: Color,
    val accent: Color,
    val title: Color,
    val text: Color,
)

private const val MESSAGE_HOST_Z_INDEX = 10f

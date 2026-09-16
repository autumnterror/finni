package github.detrig.core.mvvm.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Queue

/**
 * Compose-эффект для одноразовых команд.
 */
@Composable
@NonRestartableComposable
fun <T : Any> CommandsQueueEffect(
    commands: ImmutableCommandsQueue<T>,
    block: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(commands, lifecycleOwner) {
        val observer = Observer<Queue<T>> { events ->
            val iterator = events.iterator()
            while (iterator.hasNext()) {
                block.invoke(iterator.next())
                iterator.remove()
            }
        }
        commands.queue.observe(lifecycleOwner, observer)
        onDispose { commands.queue.removeObserver(observer) }
    }
}

@Immutable
class ImmutableCommandsQueue<T>(
    val queue: CommandsQueue<T>,
)

package github.detrig.core.mvvm.command

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.LinkedList
import java.util.Queue

/**
 * LiveData-очередь, которая доставляет события один раз.
 */
class CommandsQueue<T> : MutableLiveData<Queue<T>>() {

    fun onNext(value: T) {
        val commands = getValue() ?: LinkedList()
        commands.add(value)
        setValue(commands)
    }
}

inline fun <T : Any> LifecycleOwner.observe(
    liveData: CommandsQueue<T>,
    crossinline block: (T) -> Unit,
) {
    liveData.observe(this, Observer { events ->
        val iterator = events.iterator()
        while (iterator.hasNext()) {
            block.invoke(iterator.next())
            iterator.remove()
        }
    })
}

package github.detrig.feature.room.presentation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

/** Keeps app-entry detection process-scoped so recreating the room screen is not another entry. */
internal object RoomAppEntryCoordinator {
    private val lock = Any()
    private val lifecycle = ProcessLifecycleOwner.get().lifecycle
    private var processStarted = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    private var pendingEntry = processStarted
    private val observer = LifecycleEventObserver { _, event ->
        synchronized(lock) {
            when (event) {
                Lifecycle.Event.ON_STOP -> processStarted = false
                Lifecycle.Event.ON_START -> if (!processStarted) {
                    processStarted = true
                    pendingEntry = true
                }
                else -> Unit
            }
        }
    }

    init {
        lifecycle.addObserver(observer)
    }

    fun initialize() = Unit

    fun consumePendingEntry(): Boolean = synchronized(lock) {
        if (!pendingEntry) return@synchronized false
        pendingEntry = false
        true
    }
}

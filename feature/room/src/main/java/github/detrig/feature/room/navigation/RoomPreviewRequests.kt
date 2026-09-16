package github.detrig.feature.room.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Одноразовый запрос из соседней фичи, ожидающий возвращения в комнату. */
internal class RoomPreviewRequests {
    private val pending = MutableStateFlow<String?>(null)
    val zoneId = pending.asStateFlow()
    fun request(zoneId: String) { pending.value = zoneId }
    fun consume(zoneId: String) { pending.compareAndSet(zoneId, null) }
}

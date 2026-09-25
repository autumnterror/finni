package github.detrig.feature.shop.domain

import github.detrig.products.StoreId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class ShopDecisionEventStore {
    private val events = MutableStateFlow<Map<StoreId, ShopDecisionEvent>>(emptyMap())

    fun observe(storeId: StoreId): Flow<ShopDecisionEvent?> = events
        .map { it[storeId] }
        .distinctUntilChanged()

    fun set(event: ShopDecisionEvent?) {
        if (event == null) return
        events.update { it + (event.storeId to event) }
    }

    fun replace(storeId: StoreId, event: ShopDecisionEvent?) {
        require(event == null || event.storeId == storeId)
        events.update { current ->
            if (event == null) current - storeId else current + (storeId to event)
        }
    }

    fun current(storeId: StoreId): ShopDecisionEvent? = events.value[storeId]

    fun clear(storeId: StoreId) {
        events.update { it - storeId }
    }
}

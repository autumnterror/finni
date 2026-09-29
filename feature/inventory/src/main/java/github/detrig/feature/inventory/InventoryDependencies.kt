package github.detrig.feature.inventory

import github.detrig.core.infrastructure.preferences.SharedStorage
import kotlinx.coroutines.flow.Flow

interface InventoryDependencies {
    fun storage(): SharedStorage
    suspend fun currentAbsoluteDay(): Long
    fun observeAbsoluteDay(): Flow<Long>
}

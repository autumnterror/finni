package github.detrig.feature.inventory

import github.detrig.core.infrastructure.preferences.SharedStorage

interface InventoryDependencies {
    fun storage(): SharedStorage
}

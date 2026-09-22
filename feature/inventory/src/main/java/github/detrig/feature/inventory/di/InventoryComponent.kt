package github.detrig.feature.inventory.di

import github.detrig.feature.inventory.api.InventoryApi

internal interface InventoryComponent {
    val api: InventoryApi
}

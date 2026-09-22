package github.detrig.feature.inventory.di

import github.detrig.feature.inventory.InventoryDependencies
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.inventory.api.InventoryApiImpl
import github.detrig.feature.inventory.data.InventoryRepositoryImpl

internal class InventoryModule(
    dependencies: InventoryDependencies,
) : InventoryComponent {
    private val repository = InventoryRepositoryImpl(dependencies.storage())

    override val api: InventoryApi = InventoryApiImpl(repository)
}

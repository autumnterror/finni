package github.detrig.feature.inventory.di

import github.detrig.feature.inventory.InventoryDependencies
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.inventory.api.InventoryApiImpl
import github.detrig.feature.inventory.data.InventoryRepositoryImpl

internal class InventoryModule(
    dependencies: InventoryDependencies,
) : InventoryComponent {
    private val repository = InventoryRepositoryImpl(
        storage = dependencies.storage(),
        currentAbsoluteDay = dependencies::currentAbsoluteDay,
        observeAbsoluteDay = dependencies::observeAbsoluteDay,
    )

    override val api: InventoryApi = InventoryApiImpl(repository)
}

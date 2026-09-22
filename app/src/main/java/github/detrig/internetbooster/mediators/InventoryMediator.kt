package github.detrig.internetbooster.mediators

import android.content.Context
import androidx.annotation.MainThread
import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.inventory.InventoryDependencies
import github.detrig.feature.inventory.InventoryFeature
import github.detrig.feature.inventory.api.InventoryApi

internal class InventoryMediator(
    private val coreComponent: CoreComponent,
) : Mediator<InventoryApi> {
    @MainThread
    fun init() {
        InventoryFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : InventoryDependencies {
                override fun storage(): SharedStorage = SharedStorage(
                    coreComponent.context.getSharedPreferences(
                        INVENTORY_PREFERENCES,
                        Context.MODE_PRIVATE,
                    ),
                )
            }
        }
    }

    @MainThread
    override fun getApi(): InventoryApi = InventoryFeature.getApi()

    private companion object {
        const val INVENTORY_PREFERENCES = "inventory_food_storage"
    }
}

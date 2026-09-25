package github.detrig.internetbooster.mediators

import android.content.Context
import android.content.SharedPreferences
import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.pet.PetDependencies
import github.detrig.feature.pet.PetFeature
import github.detrig.feature.pet.api.PetApi

internal class PetMediator(
    private val coreComponent: CoreComponent,
    private val gameStateMediator: GameStateMediator,
) : Mediator<PetApi> {
    fun init() {
        PetFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : PetDependencies {
                override fun profilePreferences(): SharedPreferences =
                    coreComponent.context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                override fun assets() = coreComponent.context.assets
                override fun progressionApi() = gameStateMediator.getProgressionApi()
            }
        }
    }

    override fun getApi(): PetApi = PetFeature.getApi()

    private companion object {
        const val PREFERENCES_NAME = "finpet_pet_profile"
    }
}

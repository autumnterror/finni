package github.detrig.internetbooster.mediators

import androidx.annotation.MainThread
import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.phone.PhoneDependencies
import github.detrig.feature.phone.PhoneFeature
import github.detrig.feature.phone.api.PhoneApi

internal class PhoneMediator(
    private val coreComponent: CoreComponent,
    private val roomMediator: RoomMediator,
    private val petMediator: PetMediator,
    private val shopMediator: ShopMediator,
) : Mediator<PhoneApi> {

    @MainThread
    fun init() {
        PhoneFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : PhoneDependencies {
                override fun globalNavigator() = coreComponent.globalNavigator
                override fun roomApi() = roomMediator.getApi()
                override fun petApi() = petMediator.getApi()
                override fun shopApi() = shopMediator.getApi()
            }
        }
    }

    @MainThread
    override fun getApi(): PhoneApi = PhoneFeature.getApi()
}

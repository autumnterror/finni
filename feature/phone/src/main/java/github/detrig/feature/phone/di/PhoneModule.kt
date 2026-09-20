package github.detrig.feature.phone.di

import github.detrig.feature.phone.PhoneDependencies
import github.detrig.feature.phone.api.PhoneApi
import github.detrig.feature.phone.api.PhoneApiImpl
import github.detrig.feature.phone.navigation.PhoneRouter
import github.detrig.feature.phone.navigation.PhoneRouterImpl

internal class PhoneModule(
    dependencies: PhoneDependencies,
) : PhoneComponent {
    override val roomApi = dependencies.roomApi()
    override val petApi = dependencies.petApi()
    override val shopApi = dependencies.shopApi()

    override val router: PhoneRouter by lazy {
        PhoneRouterImpl(dependencies.globalNavigator())
    }

    override val api: PhoneApi by lazy {
        PhoneApiImpl(router)
    }
}

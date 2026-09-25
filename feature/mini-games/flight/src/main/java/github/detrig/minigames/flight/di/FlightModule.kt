package github.detrig.minigames.flight.di

import github.detrig.minigames.flight.FlightDependencies
import github.detrig.minigames.flight.api.FlightApiImpl
import github.detrig.minigames.flight.data.RoomFlightRepository
import github.detrig.minigames.flight.domain.*
import github.detrig.minigames.flight.navigation.FlightRouterImpl
import github.detrig.minigames.flight.presentation.FlightViewModel

internal class FlightModule(private val dependencies: FlightDependencies) : FlightComponent {
    private val config by lazy { FlightConfig.decode(dependencies.configurationJson()) }
    private val router by lazy { FlightRouterImpl(dependencies.globalNavigator()) }
    private val repository by lazy {
        RoomFlightRepository(dependencies.flightDao(), dependencies.transactionRunner(), config)
    }
    private val interactor by lazy { FlightInteractor(repository, dependencies.host()) }
    override val api by lazy { FlightApiImpl(router) }
    override val petApi by lazy { dependencies.petApi() }
    override val gameAudio by lazy { dependencies.gameAudio() }
    override fun viewModel() = FlightViewModel(FlightEngine(config), interactor, router, dependencies::currentTimeMillis)
}

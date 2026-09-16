package github.detrig.feature.productmarket.domain

import github.detrig.feature.productmarket.api.ProductMarketHost
import kotlinx.coroutines.flow.onStart

internal class RestoreMarketTripInteractor(private val repository: MarketTripRepository, private val rules: MarketRules) {
    suspend operator fun invoke(): MarketTrip {
        val restored = repository.load()?.let(rules::restore) ?: rules.newTrip()
        repository.save(restored)
        return restored
    }
}

internal class ObserveMarketBalanceInteractor(private val host: ProductMarketHost) {
    operator fun invoke() = host.observeBalanceRub().onStart { host.preparePlayer() }
}

internal class FinishMarketTripInteractor(private val repository: MarketTripRepository, private val rules: MarketRules) {
    suspend operator fun invoke(trip: MarketTrip): MarketTrip = rules.finish(trip).also { repository.save(it) }
}

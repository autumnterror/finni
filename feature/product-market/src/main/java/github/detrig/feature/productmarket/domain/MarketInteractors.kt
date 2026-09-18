package github.detrig.feature.productmarket.domain

import github.detrig.feature.productmarket.api.ProductMarketHost
import github.detrig.feature.productmarket.api.MarketPaymentResult
import github.detrig.products.ProductCatalog
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

internal sealed interface CheckoutResult {
    data class Finished(val trip: MarketTrip) : CheckoutResult
    data class InsufficientFunds(val missingRub: Long) : CheckoutResult
    data object Rejected : CheckoutResult
}

internal class CheckoutMarketTripInteractor(
    private val repository: MarketTripRepository,
    private val rules: MarketRules,
    private val catalog: ProductCatalog,
    private val host: ProductMarketHost,
) {
    suspend operator fun invoke(trip: MarketTrip): CheckoutResult {
        val totalRub = catalog.quote(trip.cart.map { (id, quantity) -> github.detrig.products.ProductQuantity(id, quantity) }).totalRub
        return when (val payment = host.payForCart(trip.id, totalRub)) {
            MarketPaymentResult.Paid,
            MarketPaymentResult.AlreadyPaid -> CheckoutResult.Finished(rules.finish(trip).also { repository.save(it) })
            is MarketPaymentResult.InsufficientFunds -> CheckoutResult.InsufficientFunds(payment.missingRub)
            MarketPaymentResult.Rejected -> CheckoutResult.Rejected
        }
    }
}

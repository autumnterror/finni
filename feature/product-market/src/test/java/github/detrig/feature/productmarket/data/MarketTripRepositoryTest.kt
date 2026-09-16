package github.detrig.feature.productmarket.data

import github.detrig.feature.productmarket.domain.*
import github.detrig.products.DefaultProductCatalog
import github.detrig.products.ProductIds
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketTripRepositoryTest {
    private val rules = MarketRules(MarketConfiguration(), DefaultProductCatalog()) { "trip" }

    @Test fun queuedCheckpointCannotOverwriteFinalResult() = runTest {
        val gate = CompletableDeferred<Unit>()
        val dao = FakeDao()
        dao.gate = gate
        val repo = MarketTripRepositoryImpl(dao, MarketTripCodec(), backgroundScope)
        val initial = rules.newTrip()
        repo.checkpoint(initial)
        val picked = initial.copy(cart = mapOf(ProductIds.Carrot to 2))
        repo.checkpoint(picked)
        val finished = rules.finish(picked.copy(phase = MarketPhase.CHECKOUT, distance = rules.config.endDistance))
        val saving = async { repo.save(finished) }
        runCurrent()
        gate.complete(Unit)
        saving.await()
        assertEquals(finished, repo.load())
        assertEquals(finished.lastResult, repo.observeLastResult().first())
    }

    @Test fun failedCheckpointIsObservableAndRetryPreservesGoods() = runTest {
        val dao = FakeDao()
        val repo = MarketTripRepositoryImpl(dao, MarketTripCodec(), backgroundScope)
        val trip = rules.newTrip().copy(cart = mapOf(ProductIds.Apple to 1))
        dao.fail = true
        repo.checkpoint(trip)
        runCurrent()
        assertTrue(repo.saveFailed.value)
        dao.fail = false
        repo.save(trip)
        assertFalse(repo.saveFailed.value)
        assertEquals(trip, repo.load())
    }

    @Test fun invalidSavedJsonIsNotReplacedWithNewProgress() = runTest {
        val dao = FakeDao()
        val bad = MarketTripEntity(snapshotJson = "invalid")
        dao.row.value = bad
        val repo = MarketTripRepositoryImpl(dao, MarketTripCodec(), backgroundScope)
        val result = runCatching { RestoreMarketTripInteractor(repo, rules)() }
        assertTrue(result.isFailure)
        assertEquals(bad, dao.row.value)
    }

    private class FakeDao : MarketTripDao {
        val row = MutableStateFlow<MarketTripEntity?>(null)
        var gate: CompletableDeferred<Unit>? = null
        var fail = false
        override suspend fun read() = row.value
        override fun observe(): Flow<MarketTripEntity?> = row
        override suspend fun save(entity: MarketTripEntity) {
            gate?.await()
            check(!fail) { "Disk unavailable" }
            row.value = entity
        }
    }
}

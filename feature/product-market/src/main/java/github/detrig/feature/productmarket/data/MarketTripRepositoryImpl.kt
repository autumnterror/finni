package github.detrig.feature.productmarket.data

import github.detrig.feature.productmarket.domain.MarketTrip
import github.detrig.feature.productmarket.domain.MarketTripRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MarketTripRepositoryImpl(
    private val dao: MarketTripDao,
    private val codec: MarketTripCodec,
    private val applicationScope: CoroutineScope,
) : MarketTripRepository {
    private val mutex = Mutex()
    private val failed = MutableStateFlow(false)
    override val saveFailed = failed.asStateFlow()

    override suspend fun load(): MarketTrip? = mutex.withLock { dao.read()?.let { codec.decode(it.snapshotJson) } }

    override suspend fun save(trip: MarketTrip) = mutex.withLock {
        dao.save(MarketTripEntity(snapshotJson = codec.encode(trip)))
        failed.value = false
    }

    override fun checkpoint(trip: MarketTrip) {
        // Захватываем очередь до возврата: старый кадр не перезапишет более новый выбор.
        // applicationScope завершит запись и после ухода с экрана.
        applicationScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try { save(trip) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { failed.value = true }
        }
    }

    override fun observeLastResult() = dao.observe()
        .map { it?.let { row -> codec.decode(row.snapshotJson).lastResult } }
        .distinctUntilChanged()
}

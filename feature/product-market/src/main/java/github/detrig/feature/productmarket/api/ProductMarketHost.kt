package github.detrig.feature.productmarket.api

import kotlinx.coroutines.flow.Flow

/** Только чтение общего состояния: подбор в магазине не является оплатой. */
interface ProductMarketHost {
    suspend fun preparePlayer()
    fun observeBalanceRub(): Flow<Int>
}

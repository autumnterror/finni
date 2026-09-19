package github.detrig.feature.shop.presentation

import github.detrig.products.StoreId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Keeps the short-lived receipt visible while navigation returns from cart to the catalog. */
internal class ShopReceiptStore {
    private val receipts = MutableStateFlow<Map<StoreId, ShopReceipt>>(emptyMap())

    fun observe(storeId: StoreId): Flow<ShopReceipt?> =
        receipts.map { receiptsByStore -> receiptsByStore[storeId] }.distinctUntilChanged()

    fun show(storeId: StoreId, receipt: ShopReceipt) {
        receipts.update { receiptsByStore -> receiptsByStore + (storeId to receipt) }
    }

    fun clear(storeId: StoreId) {
        receipts.update { receiptsByStore -> receiptsByStore - storeId }
    }
}

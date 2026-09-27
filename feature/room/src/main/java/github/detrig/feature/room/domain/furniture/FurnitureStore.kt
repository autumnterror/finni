package github.detrig.feature.room.domain.furniture

import android.content.SharedPreferences
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.domain.PaymentClassification
import github.detrig.feature.planning.domain.PlanActualOperation
import github.detrig.feature.week.api.WeekApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

internal data class FurnitureOwnership(
    val owned: Set<String> = emptySet(),
    val equipped: Map<String, String> = emptyMap(),
) {
    fun owns(id: String): Boolean = id in owned
}

internal sealed interface FurniturePurchaseResult {
    data class Purchased(val balanceRub: Long) : FurniturePurchaseResult
    data object AlreadyOwned : FurniturePurchaseResult
    data object InsufficientFunds : FurniturePurchaseResult
    data object Failed : FurniturePurchaseResult
}

/** The economy operation is the durable purchase receipt; a pending marker retries delivery after a crash. */
internal class FurnitureStore(
    private val preferences: SharedPreferences,
    private val catalog: FurnitureCatalog,
    private val economy: EconomyApi,
    private val planning: PlanningApi,
    private val week: WeekApi,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(readOwnership())
    val state: StateFlow<FurnitureOwnership> = mutableState

    suspend fun reconcilePending() = mutex.withLock { reconcilePendingLocked() }

    private suspend fun reconcilePendingLocked() {
        val pending = readPending() ?: return
        val variant = catalog.byId[pending.first]
        if (variant == null) {
            clearPending()
        } else {
            applyPurchase(variant, pending.second)
        }
    }

    suspend fun purchase(id: String): FurniturePurchaseResult = mutex.withLock {
        reconcilePendingLocked()
        if (readPending() != null) return@withLock FurniturePurchaseResult.Failed
        val variant = catalog.byId[id] ?: return@withLock FurniturePurchaseResult.Failed
        if (id in mutableState.value.owned) return@withLock FurniturePurchaseResult.AlreadyOwned
        val weekNumber = week.initialize().weekNumber
        writePending(id, weekNumber)
        applyPurchase(variant, weekNumber)
    }

    suspend fun equip(slotId: String, id: String?): Boolean = mutex.withLock {
        if (catalog.bySlot[slotId] == null) return@withLock false
        if (id != null && (catalog.byId[id]?.slotId != slotId || id !in mutableState.value.owned)) {
            return@withLock false
        }
        val equipped = mutableState.value.equipped.toMutableMap()
        if (id == null) equipped.remove(slotId) else equipped[slotId] = id
        writeOwnership(mutableState.value.copy(equipped = equipped))
        true
    }

    private suspend fun applyPurchase(variant: FurnitureVariant, weekNumber: Long): FurniturePurchaseResult {
        val operationId = "interior:${variant.id}:purchase"
        val result = economy.debit(
            operationId = operationId,
            amountRub = variant.priceRub,
            context = OperationContext(reasonId = "shop:interior:purchase", metadata = variant.id),
        )
        if (result is FinancialOperationResult.Rejected) {
            clearPending()
            return if (result.reason == github.detrig.feature.economy.domain.RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS) {
                FurniturePurchaseResult.InsufficientFunds
            } else FurniturePurchaseResult.Failed
        }
        val ownership = mutableState.value
        if (variant.id !in ownership.owned) {
            writeOwnership(
                ownership.copy(
                    owned = ownership.owned + variant.id,
                    equipped = ownership.equipped + (variant.slotId to variant.id),
                ),
            )
        }
        try {
            if (planning.getPlanProgress(weekNumber) != null) {
                planning.recordActual(
                    PlanActualOperation.Payment(
                        operationId = operationId,
                        weekNumber = weekNumber,
                        amountRub = variant.priceRub,
                        classification = PaymentClassification.OPTIONAL,
                    ),
                )
            }
            clearPending()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The pending marker retries this projection; the economy operation already owns the purchase.
        }
        return FurniturePurchaseResult.Purchased(result.state.availableRub)
    }

    private fun readOwnership(): FurnitureOwnership {
        val raw = preferences.getString(STATE_KEY, null) ?: return FurnitureOwnership()
        return runCatching {
            val json = JSONObject(raw)
            val ids = json.optJSONArray("owned") ?: JSONArray()
            val owned = (0 until ids.length()).mapNotNull { ids.optString(it).takeIf(catalog.byId::containsKey) }.toSet()
            val equippedJson = json.optJSONObject("equipped") ?: JSONObject()
            val equipped = buildMap {
                val keys = equippedJson.keys()
                while (keys.hasNext()) {
                    val slotId = keys.next()
                    val id = equippedJson.optString(slotId)
                    if (id in owned && catalog.byId[id]?.slotId == slotId) put(slotId, id)
                }
            }
            FurnitureOwnership(owned, equipped)
        }.getOrDefault(FurnitureOwnership())
    }

    private fun writeOwnership(value: FurnitureOwnership) {
        val json = JSONObject().apply {
            put("owned", JSONArray(value.owned.sorted()))
            put("equipped", JSONObject(value.equipped))
        }
        check(preferences.edit().putString(STATE_KEY, json.toString()).commit())
        mutableState.value = value
    }

    private fun readPending(): Pair<String, Long>? {
        val id = preferences.getString(PENDING_ID_KEY, null) ?: return null
        return id to preferences.getLong(PENDING_WEEK_KEY, 1L)
    }

    private fun writePending(id: String, weekNumber: Long) {
        check(preferences.edit().putString(PENDING_ID_KEY, id).putLong(PENDING_WEEK_KEY, weekNumber).commit())
    }

    private fun clearPending() {
        check(preferences.edit().remove(PENDING_ID_KEY).remove(PENDING_WEEK_KEY).commit())
    }

    private companion object {
        const val STATE_KEY = "interior_ownership_v1"
        const val PENDING_ID_KEY = "interior_pending_id_v1"
        const val PENDING_WEEK_KEY = "interior_pending_week_v1"
    }
}

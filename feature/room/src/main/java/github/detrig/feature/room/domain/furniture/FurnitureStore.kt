package github.detrig.feature.room.domain.furniture

import android.content.SharedPreferences
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.domain.PaymentClassification
import github.detrig.feature.planning.domain.PlanActualOperation
import github.detrig.feature.room.domain.surface.SurfaceCatalog
import github.detrig.feature.room.domain.surface.SurfaceKind
import github.detrig.feature.room.domain.surface.surfaceSlotId
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
    val ownedSurfaces: Set<String> = emptySet(),
    val equippedSurfaces: Map<String, String> = emptyMap(),
) {
    fun owns(id: String): Boolean = id in owned
    fun ownsSurface(id: String): Boolean = id in ownedSurfaces
}

internal sealed interface FurniturePurchaseResult {
    data class Purchased(val balanceRub: Long) : FurniturePurchaseResult
    data object AlreadyOwned : FurniturePurchaseResult
    data object InsufficientFunds : FurniturePurchaseResult
    data object Failed : FurniturePurchaseResult
}

/** One durable economy path owns purchases of furniture and room surfaces. */
internal class FurnitureStore(
    private val preferences: SharedPreferences,
    private val catalog: FurnitureCatalog,
    private val surfaceCatalog: SurfaceCatalog,
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
        val item = itemFor(pending.first)
        if (item == null) clearPending() else applyPurchase(item, pending.second)
    }

    suspend fun purchase(id: String): FurniturePurchaseResult = purchaseItem(id, surface = false)

    suspend fun purchaseSurface(id: String): FurniturePurchaseResult = purchaseItem(id, surface = true)

    private suspend fun purchaseItem(id: String, surface: Boolean): FurniturePurchaseResult = mutex.withLock {
        reconcilePendingLocked()
        if (readPending() != null) return@withLock FurniturePurchaseResult.Failed
        val item = itemFor(id)?.takeIf { it.surface == surface } ?: return@withLock FurniturePurchaseResult.Failed
        if (if (surface) id in mutableState.value.ownedSurfaces else id in mutableState.value.owned) {
            return@withLock FurniturePurchaseResult.AlreadyOwned
        }
        val weekNumber = week.initialize().weekNumber
        writePending(id, weekNumber)
        applyPurchase(item, weekNumber)
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

    suspend fun equipSurface(roomId: String, kind: SurfaceKind, id: String?): Boolean = mutex.withLock {
        val targetSlot = surfaceSlotId(roomId, kind)
        if (id != null && (surfaceCatalog.byId[id]?.slotId != targetSlot || id !in mutableState.value.ownedSurfaces)) {
            return@withLock false
        }
        val equipped = mutableState.value.equippedSurfaces.toMutableMap()
        if (id == null) equipped.remove(targetSlot) else equipped[targetSlot] = id
        writeOwnership(mutableState.value.copy(equippedSurfaces = equipped))
        true
    }

    private data class PurchaseItem(val id: String, val priceRub: Long, val slotId: String, val surface: Boolean)

    private fun itemFor(id: String): PurchaseItem? = catalog.byId[id]?.let {
        PurchaseItem(it.id, it.priceRub, it.slotId, false)
    } ?: surfaceCatalog.byId[id]?.let { PurchaseItem(it.id, it.priceRub, it.slotId, true) }

    private suspend fun applyPurchase(item: PurchaseItem, weekNumber: Long): FurniturePurchaseResult {
        val operationId = "interior:${item.id}:purchase"
        val result = economy.debit(
            operationId = operationId,
            amountRub = item.priceRub,
            context = OperationContext(reasonId = "shop:interior:purchase", metadata = item.id),
        )
        if (result is FinancialOperationResult.Rejected) {
            clearPending()
            return if (result.reason == github.detrig.feature.economy.domain.RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS) {
                FurniturePurchaseResult.InsufficientFunds
            } else FurniturePurchaseResult.Failed
        }
        val ownership = mutableState.value
        if (item.surface && item.id !in ownership.ownedSurfaces) {
            writeOwnership(ownership.copy(
                ownedSurfaces = ownership.ownedSurfaces + item.id,
                equippedSurfaces = ownership.equippedSurfaces + (item.slotId to item.id),
            ))
        } else if (!item.surface && item.id !in ownership.owned) {
            writeOwnership(ownership.copy(
                owned = ownership.owned + item.id,
                equipped = ownership.equipped + (item.slotId to item.id),
            ))
        }
        try {
            if (planning.getPlanProgress(weekNumber) != null) {
                planning.recordActual(
                    PlanActualOperation.Payment(
                        operationId = operationId,
                        weekNumber = weekNumber,
                        amountRub = item.priceRub,
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
            val surfaceIds = json.optJSONArray("owned_surfaces") ?: JSONArray()
            val ownedSurfaces = (0 until surfaceIds.length())
                .mapNotNull { surfaceIds.optString(it).takeIf(surfaceCatalog.byId::containsKey) }.toSet()
            val equippedJson = json.optJSONObject("equipped") ?: JSONObject()
            val equipped = buildMap {
                val keys = equippedJson.keys()
                while (keys.hasNext()) {
                    val slotId = keys.next()
                    val id = equippedJson.optString(slotId)
                    if (id in owned && catalog.byId[id]?.slotId == slotId) put(slotId, id)
                }
            }
            val equippedSurfacesJson = json.optJSONObject("equipped_surfaces") ?: JSONObject()
            val equippedSurfaces = buildMap {
                val keys = equippedSurfacesJson.keys()
                while (keys.hasNext()) {
                    val surfaceSlot = keys.next()
                    val id = equippedSurfacesJson.optString(surfaceSlot)
                    if (id in ownedSurfaces && surfaceCatalog.byId[id]?.slotId == surfaceSlot) put(surfaceSlot, id)
                }
            }
            FurnitureOwnership(owned, equipped, ownedSurfaces, equippedSurfaces)
        }.getOrDefault(FurnitureOwnership())
    }

    private fun writeOwnership(value: FurnitureOwnership) {
        val json = JSONObject().apply {
            put("owned", JSONArray(value.owned.sorted()))
            put("equipped", JSONObject(value.equipped))
            put("owned_surfaces", JSONArray(value.ownedSurfaces.sorted()))
            put("equipped_surfaces", JSONObject(value.equippedSurfaces))
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

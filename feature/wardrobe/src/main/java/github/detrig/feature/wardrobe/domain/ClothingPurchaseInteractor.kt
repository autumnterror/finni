package github.detrig.feature.wardrobe.domain

import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.pet.api.ClothingItem
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.domain.PaymentClassification
import github.detrig.feature.planning.domain.PlanActualOperation
import github.detrig.feature.week.api.WeekApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal sealed interface ClothingPurchaseResult {
    data object Purchased : ClothingPurchaseResult
    data object AlreadyOwned : ClothingPurchaseResult
    data class NotEnoughMoney(val missingRub: Long) : ClothingPurchaseResult
    data object Failed : ClothingPurchaseResult
}

internal class ClothingPurchaseInteractor(
    private val pet: PetApi,
    private val economy: EconomyApi,
    private val planning: PlanningApi,
    private val week: WeekApi,
) {
    private val mutex = Mutex()

    suspend fun reconcile() = mutex.withLock { reconcileCommittedPurchases() }

    suspend fun purchase(item: ClothingItem): ClothingPurchaseResult = mutex.withLock {
        reconcileCommittedPurchases()
        if (item.id in requireNotNull(pet.observeProfile().first()).clothing.ownedIds) {
            return@withLock ClothingPurchaseResult.AlreadyOwned
        }
        val weekNumber = week.initialize().weekNumber
        val result = economy.debit(
            operationId = operationId(item.id),
            amountRub = item.priceRub,
            context = OperationContext(reasonId = reasonId(item.id), metadata = weekNumber.toString()),
        )
        when (result) {
            is FinancialOperationResult.Applied,
            is FinancialOperationResult.AlreadyApplied -> {
                pet.recordClothingPurchase(item.id)
                pet.equipClothing(item.slot, item.id)
                tryRecordPlanActual(operationId(item.id), weekNumber, item.priceRub)
                ClothingPurchaseResult.Purchased
            }
            is FinancialOperationResult.Rejected -> when (result.reason) {
                RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS ->
                    ClothingPurchaseResult.NotEnoughMoney((item.priceRub - result.state.availableRub).coerceAtLeast(1))
                else -> ClothingPurchaseResult.Failed
            }
        }
    }

    private suspend fun reconcileCommittedPurchases() {
        val itemsById = pet.clothingItems().associateBy(ClothingItem::id)
        economy.getExpenseHistory().sortedBy { it.timestampMillis }.forEach { operation ->
            val id = operation.context.reasonId?.removePrefix(REASON_PREFIX)
                ?.takeIf { operation.context.reasonId == reasonId(it) && it in itemsById }
                ?: return@forEach
            val profile = requireNotNull(pet.observeProfile().first())
            if (id !in profile.clothing.ownedIds) {
                pet.recordClothingPurchase(id)
                val slot = itemsById.getValue(id).slot
                pet.equipClothing(slot, id)
            }
            operation.context.metadata?.toLongOrNull()?.takeIf { it > 0 }?.let { weekNumber ->
                tryRecordPlanActual(operation.id, weekNumber, operation.amountRub)
            }
        }
    }

    private suspend fun tryRecordPlanActual(operationId: String, weekNumber: Long, amountRub: Long) {
        try {
            if (planning.getPlanProgress(weekNumber) != null) {
                planning.recordActual(
                    PlanActualOperation.Payment(
                        operationId = operationId,
                        weekNumber = weekNumber,
                        amountRub = amountRub,
                        classification = PaymentClassification.OPTIONAL,
                    ),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Economy history is the durable retry source for this optional projection.
        }
    }

    private fun operationId(itemId: String) = "clothing_purchase:$itemId"
    private fun reasonId(itemId: String) = "$REASON_PREFIX$itemId"

    private companion object { const val REASON_PREFIX = "clothing:" }
}

package github.detrig.feature.economy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "economy_state")
data class EconomyStateEntity(
    @PrimaryKey val id: String = CURRENT_ID,
    val availableRub: Long,
    val savingsRub: Long,
    val debtRub: Long,
    val periodicAmountRub: Long,
    val periodicPeriodMillis: Long,
    val nextPeriodicAtMillis: Long,
) {
    companion object { const val CURRENT_ID = "current" }
}

@Entity(tableName = "financial_operations")
data class FinancialOperationEntity(
    @PrimaryKey val id: String,
    val typeCode: String,
    val amountRub: Long,
    val timestampMillis: Long,
    val availableDeltaRub: Long,
    val savingsDeltaRub: Long,
    val debtDeltaRub: Long,
    val beforeAvailableRub: Long,
    val beforeSavingsRub: Long,
    val beforeDebtRub: Long,
    val afterAvailableRub: Long,
    val afterSavingsRub: Long,
    val afterDebtRub: Long,
    val reasonId: String?,
    val metadata: String?,
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val targetRub: Long,
    val metadata: String?,
    val isActive: Boolean,
)

@Entity(tableName = "parent_help_state")
data class ParentHelpStateEntity(
    @PrimaryKey val id: String = CURRENT_ID,
    val offerId: String,
    val receivedRub: Long,
    val totalRepaymentRub: Long,
    val remainingRub: Long,
    val paymentsRemaining: Int,
) {
    companion object { const val CURRENT_ID = "current" }
}

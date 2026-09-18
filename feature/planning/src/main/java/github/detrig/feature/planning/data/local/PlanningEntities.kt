package github.detrig.feature.planning.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_plans")
data class WeeklyPlanEntity(
    @PrimaryKey val weekNumber: Long,
    val availableRub: Long,
    val mandatoryPercent: Int,
    val wantsPercent: Int,
    val savingsPercent: Int,
)

@Entity(tableName = "plan_actual_operations")
data class PlanActualOperationEntity(
    @PrimaryKey val operationId: String,
    val weekNumber: Long,
    val categoryCode: String,
    val amountRub: Long,
)

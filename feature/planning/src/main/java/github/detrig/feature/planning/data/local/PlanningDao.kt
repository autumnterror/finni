package github.detrig.feature.planning.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanningDao {
    @Query("SELECT * FROM weekly_plans WHERE weekNumber = :weekNumber")
    suspend fun getPlan(weekNumber: Long): WeeklyPlanEntity?

    @Query("SELECT * FROM weekly_plans WHERE weekNumber = :weekNumber")
    fun observePlan(weekNumber: Long): Flow<WeeklyPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlan(plan: WeeklyPlanEntity)

    @Query("SELECT * FROM plan_actual_operations WHERE weekNumber = :weekNumber ORDER BY operationId")
    suspend fun getActualOperations(weekNumber: Long): List<PlanActualOperationEntity>

    @Query("SELECT * FROM plan_actual_operations WHERE weekNumber = :weekNumber ORDER BY operationId")
    fun observeActualOperations(weekNumber: Long): Flow<List<PlanActualOperationEntity>>

    @Query("SELECT * FROM plan_actual_operations WHERE operationId = :operationId")
    suspend fun getActualOperation(operationId: String): PlanActualOperationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActualOperation(operation: PlanActualOperationEntity)
}

package github.detrig.feature.economy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EconomyDao {
    @Query("SELECT * FROM economy_state WHERE id = 'current'")
    suspend fun getState(): EconomyStateEntity?

    @Query("SELECT * FROM economy_state WHERE id = 'current'")
    fun observeState(): Flow<EconomyStateEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialState(state: EconomyStateEntity): Long

    @Update
    suspend fun updateState(state: EconomyStateEntity)

    @Query("SELECT * FROM financial_operations WHERE id = :id")
    suspend fun getOperation(id: String): FinancialOperationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOperation(operation: FinancialOperationEntity)

    @Query("SELECT * FROM financial_operations ORDER BY timestampMillis ASC, id ASC")
    suspend fun getOperations(): List<FinancialOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: SavingsGoalEntity)

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoal(id: String): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals ORDER BY id ASC")
    suspend fun getGoals(): List<SavingsGoalEntity>

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoal(id: String): Int
}

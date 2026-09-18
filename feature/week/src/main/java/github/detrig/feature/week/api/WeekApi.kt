package github.detrig.feature.week.api

import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.week.domain.WeekState
import kotlinx.coroutines.flow.Flow

interface WeekApi {
    suspend fun initialize(): WeekState
    fun observeState(): Flow<WeekState>

    /** expectedAbsoluteDay защищает от повтора одного и того же нажатия/запроса. */
    suspend fun endDay(expectedAbsoluteDay: Long): EndDayResult
}

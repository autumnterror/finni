package github.detrig.feature.week.api

import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.week.domain.EarlyWeekEndResult
import github.detrig.feature.week.domain.WeekState
import kotlinx.coroutines.flow.Flow

interface WeekApi {
    suspend fun initialize(): WeekState
    fun observeState(): Flow<WeekState>

    /** expectedAbsoluteDay защищает от повтора одного и того же нажатия/запроса. */
    suspend fun endDay(expectedAbsoluteDay: Long): EndDayResult

    /** Досрочно завершает кризисную неделю и начисляет карманные деньги новой недели. */
    suspend fun endWeekEarlyWithParentHelp(
        expectedAbsoluteDay: Long,
        minimumRequiredBalanceRub: Long,
    ): EarlyWeekEndResult
}

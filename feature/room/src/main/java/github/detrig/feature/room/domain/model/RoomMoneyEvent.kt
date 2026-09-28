package github.detrig.feature.room.domain.model

import kotlin.random.Random

/** Amounts are rubles and never scale with pocket money. */
internal data class RoomMoneyEvent(
    val id: String,
    val weekNumber: Long,
    val dayOfWeek: Int,
    val title: String,
    val amountRub: Long,
    val kind: Kind,
    val savedAllocation: MoneyAllocation? = null,
) {
    enum class Kind { KNOWN_EXPENSE, UNEXPECTED_EXPENSE, EXTRA_INCOME }
}

internal enum class MoneyAllocation { WANTS, RESERVE, GOAL, FREE }

internal sealed interface MoneyEventResolution {
    data object Completed : MoneyEventResolution
    data object InsufficientFunds : MoneyEventResolution
    data object NoActiveGoal : MoneyEventResolution
}

internal object RoomMoneyEventSchedule {
    private val positive = listOf(
        "Небольшой подарок" to 50L,
        "Помощь по дому" to 70L,
        "Подарок от родственников" to 100L,
        "Возврат денег за отменённую покупку" to 80L,
        "Небольшой денежный приз" to 120L,
        "Большой подарок" to 150L,
    )
    private val negative = listOf(
        "Нужно заменить сломанную лампу" to 50L,
        "Закончилась нужная вещь" to 60L,
        "Сломался предмет комнаты" to 80L,
        "Неожиданная обязательная покупка" to 100L,
        "Срочно нужна вещь для питомца" to 120L,
        "Непредвиденная крупная трата" to 150L,
        "Серьёзная непредвиденная проблема" to 200L,
    )

    fun knownExpenseRub(weekNumber: Long): Long =
        if (weekNumber == 2L) 80L else 0L

    fun events(weekNumber: Long, random: Random): List<RoomMoneyEvent> {
        require(weekNumber >= 1L)
        val known = if (knownExpenseRub(weekNumber) > 0) listOf(
            RoomMoneyEvent(
                id = "money-event:$weekNumber:known-mom-birthday",
                weekNumber = weekNumber,
                dayOfWeek = 3,
                title = "Подарок маме на день рождения",
                amountRub = 80L,
                kind = RoomMoneyEvent.Kind.KNOWN_EXPENSE,
            ),
        ) else emptyList()
        val ordinary = when (weekNumber) {
            1L -> emptyList()
            2L -> emptyList()
            3L -> listOf(income(weekNumber, 3, 1))
            else -> {
                val count = if (known.isNotEmpty() || random.nextBoolean()) 1 else 2
                val days = listOf(2, 3, 5, 6).shuffled(random).take(count).sorted()
                val firstIsIncome = random.nextBoolean()
                days.mapIndexed { index, day ->
                    val isIncome = if (index == 0) firstIsIncome else !firstIsIncome
                    if (isIncome) income(weekNumber, day, random.nextInt(positive.size))
                    else expense(weekNumber, day, random.nextInt(negative.size))
                }
            }
        }
        return (known + ordinary).sortedBy { it.dayOfWeek }
    }

    private fun income(week: Long, day: Int, index: Int): RoomMoneyEvent {
        val (title, amount) = positive[index]
        return RoomMoneyEvent("money-event:$week:$day:income", week, day, title, amount,
            RoomMoneyEvent.Kind.EXTRA_INCOME)
    }

    private fun expense(week: Long, day: Int, index: Int): RoomMoneyEvent {
        val (title, amount) = negative[index]
        return RoomMoneyEvent("money-event:$week:$day:expense", week, day, title, amount,
            RoomMoneyEvent.Kind.UNEXPECTED_EXPENSE)
    }
}

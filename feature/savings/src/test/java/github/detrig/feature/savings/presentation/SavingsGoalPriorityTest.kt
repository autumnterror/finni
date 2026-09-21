package github.detrig.feature.savings.presentation

import github.detrig.feature.savings.api.SavingsGoalDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class SavingsGoalPriorityTest {
    private val goals = listOf(
        SavingsGoalDraft("drawing", "Рисование", 200),
        SavingsGoalDraft("music", "Музыка", 350),
        SavingsGoalDraft("fishing", "Рыбалка", 400),
    )

    @Test
    fun selectedGameMovesToTheFirstPosition() {
        assertEquals(
            listOf("music", "drawing", "fishing"),
            goals.prioritize("music").map { it.id },
        )
    }

    @Test
    fun unknownSelectionKeepsConfiguredOrder() {
        assertEquals(goals, goals.prioritize("unknown"))
    }
}

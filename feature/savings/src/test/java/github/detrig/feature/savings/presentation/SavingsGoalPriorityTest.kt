package github.detrig.feature.savings.presentation

import github.detrig.feature.savings.api.SavingsGoalDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class SavingsGoalSelectionTest {
    private val goals = listOf(
        SavingsGoalDraft("drawing", "Рисование", 200),
        SavingsGoalDraft("music", "Музыка", 350),
        SavingsGoalDraft("fishing", "Рыбалка", 400),
    )

    @Test
    fun onlySelectedGameRemainsAvailable() {
        assertEquals(
            listOf("music"),
            goals.selected("music").map { it.id },
        )
    }

    @Test
    fun unknownSelectionShowsNoGoals() {
        assertEquals(emptyList<SavingsGoalDraft>(), goals.selected("unknown"))
    }

    @Test
    fun missingSelectionShowsNoGoals() {
        assertEquals(emptyList<SavingsGoalDraft>(), goals.selected(null))
    }
}

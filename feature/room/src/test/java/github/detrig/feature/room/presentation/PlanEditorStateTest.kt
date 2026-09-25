package github.detrig.feature.room.presentation

import github.detrig.feature.planning.domain.PlanCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanEditorStateTest {
    @Test fun movingEverySliderKeepsTheTotalWithinOneHundredAndLeavesAReserve() {
        var editor = PlanEditorState()
        editor = editor.update(PlanCategory.MANDATORY, 80)
        assertEquals(100, editor.total)
        editor = editor.update(PlanCategory.WANTS, 0)
        assertEquals(89, editor.total)
        assertEquals(11, editor.reserve)
        editor = editor.update(PlanCategory.SAVINGS, 65)
        assertEquals(100, editor.total)
        assertEquals(100, editor.toPercentages().total)
    }

    @Test fun tutorialVisitsEveryCategoryBeforeGivingControlToThePlayer() {
        val steps = generateSequence(PlanTutorialStep.MANDATORY) { it.nextOrNull() }.toList()

        assertEquals(
            listOf(
                PlanTutorialStep.MANDATORY,
                PlanTutorialStep.WANTS,
                PlanTutorialStep.SAVINGS,
                PlanTutorialStep.RESERVE,
                PlanTutorialStep.PRACTICE,
            ),
            steps,
        )
    }

    @Test fun reserveSliderKeepsAllAllocationsWithinOneHundredPercent() {
        val editor = PlanEditorState().updateReserve(40)

        assertEquals(40, editor.reserve)
        assertEquals(100, editor.total + editor.reserve)
        assertEquals(60, editor.total)
    }
}

package github.detrig.feature.room.presentation

import github.detrig.feature.planning.domain.PlanCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanEditorStateTest {
    @Test fun movingEverySliderKeepsTheTotalAtOneHundred() {
        var editor = PlanEditorState()
        editor = editor.update(PlanCategory.MANDATORY, 80)
        assertEquals(100, editor.total)
        editor = editor.update(PlanCategory.WANTS, 0)
        assertEquals(100, editor.total)
        editor = editor.update(PlanCategory.SAVINGS, 65)
        assertEquals(100, editor.total)
        assertEquals(100, editor.toPercentages().total)
    }
}

package github.detrig.feature.room.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetWashGuideStepTest {
    @Test fun dirtyNoticeContinuesToBathGuidance() {
        assertEquals(
            PetWashGuideStep.BATH_GUIDANCE,
            PetWashGuideStep.DIRTY_NOTICE.nextOrNull(),
        )
    }

    @Test fun bathGuidanceFinishesTheGuide() {
        assertNull(PetWashGuideStep.BATH_GUIDANCE.nextOrNull())
    }
}

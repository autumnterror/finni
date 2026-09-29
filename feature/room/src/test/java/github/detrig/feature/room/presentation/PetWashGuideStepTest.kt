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

    @Test fun completedGuidanceUsesNoticeWithoutBathSpotlight() {
        assertEquals(
            PetWashGuideStep.REPEAT_DIRTY_NOTICE,
            initialPetWashGuideStep(bathGuidanceCompleted = true),
        )
        assertNull(PetWashGuideStep.REPEAT_DIRTY_NOTICE.nextOrNull())
    }

    @Test fun firstDirtEpisodeStillStartsTheFullGuide() {
        assertEquals(
            PetWashGuideStep.DIRTY_NOTICE,
            initialPetWashGuideStep(bathGuidanceCompleted = false),
        )
    }
}

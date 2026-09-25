package github.detrig.feature.room.presentation

import github.detrig.feature.room.api.FirstRunOnboardingStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirstRunOnboardingStateTest {
    @Test fun planKeepsTheCameraCenteredUntilTheGameDiscoveryDialogue() {
        val state = FirstRunOnboardingState(FirstRunOnboardingStep.PLAN)

        assertNull(state.focusObjectId)
        assertEquals(emptySet<String>(), state.highlightedObjectIds)
    }

    @Test fun gameDiscoveryMovesToTheFarEdgeAndHighlightsLockedChoices() {
        val state = FirstRunOnboardingState(FirstRunOnboardingStep.GAME_DISCOVERY)

        assertEquals("drawing", state.focusObjectId)
        assertEquals(
            setOf("drawing", "music", "fishing"),
            state.highlightedObjectIds,
        )
    }

    @Test fun onlyLockedGoalChoicesAreInteractiveDuringSelection() {
        val state = FirstRunOnboardingState(FirstRunOnboardingStep.GAME_SELECTION)

        assertEquals(setOf("drawing", "music", "fishing"), state.allowedObjectIds)
    }

    @Test fun selectedGameKeepsTheCameraOnThePlayersChoice() {
        val state = FirstRunOnboardingState(
            step = FirstRunOnboardingStep.GAME_SELECTED,
            suggestedGoalZoneId = "music",
        )

        assertEquals("music", state.focusObjectId)
        assertEquals(emptySet<String>(), state.allowedObjectIds)
    }

    @Test fun piggyBankExplanationDoesNotAllowOpeningUntilItEnds() {
        val state = FirstRunOnboardingState(FirstRunOnboardingStep.PIGGY_TAP)

        assertEquals("piggy_bank", state.focusObjectId)
        assertEquals(setOf("piggy_bank"), state.highlightedObjectIds)
        assertEquals(emptySet<String>(), state.allowedObjectIds)
    }

    @Test fun piggyBankCanOnlyBeTappedAfterTheExplanation() {
        val state = FirstRunOnboardingState(FirstRunOnboardingStep.WAITING_FOR_PIGGY)

        assertEquals("piggy_bank", state.focusObjectId)
        assertEquals(setOf("piggy_bank"), state.highlightedObjectIds)
        assertEquals(setOf("piggy_bank"), state.allowedObjectIds)
    }

    @Test fun finishingTheTutorialDoesNotMoveTheCameraAgain() {
        val state = FirstRunOnboardingState(FirstRunOnboardingStep.FINISH)

        assertNull(state.focusObjectId)
        assertEquals(emptySet<String>(), state.highlightedObjectIds)
    }

    @Test fun legacyGamesStepDoesNotRepeatTheCameraMove() {
        val state = FirstRunOnboardingState(FirstRunOnboardingStep.GAMES)

        assertNull(state.focusObjectId)
        assertEquals(emptySet<String>(), state.highlightedObjectIds)
    }

    @Test fun firstCareLessonFocusesOnlyTheCurrentRoomObject() {
        val phoneExplanation = FirstRunOnboardingState(FirstRunOnboardingStep.PHONE_GUIDANCE)
        val phoneWaiting = FirstRunOnboardingState(FirstRunOnboardingStep.WAITING_FOR_PHONE)
        val fridge = FirstRunOnboardingState(FirstRunOnboardingStep.FRIDGE_GUIDANCE)
        val fridgeWaiting = FirstRunOnboardingState(FirstRunOnboardingStep.WAITING_FOR_FRIDGE)
        val table = FirstRunOnboardingState(FirstRunOnboardingStep.TABLE_GUIDANCE)

        assertEquals("phone", phoneExplanation.focusObjectId)
        assertEquals(emptySet<String>(), phoneExplanation.allowedObjectIds)
        assertEquals("phone", phoneWaiting.focusObjectId)
        assertEquals(setOf("phone"), phoneWaiting.allowedObjectIds)
        assertEquals("fridge", fridge.focusObjectId)
        assertEquals(emptySet<String>(), fridge.allowedObjectIds)
        assertEquals("fridge", fridgeWaiting.focusObjectId)
        assertEquals(setOf("fridge"), fridgeWaiting.allowedObjectIds)
        assertEquals("dining_table", table.focusObjectId)
        assertEquals(setOf("dining_table"), table.allowedObjectIds)
    }
    @Test fun bedtimeLessonFocusesAndUnlocksOnlyTheBedAtTheActionStep() {
        val explanation = FirstRunOnboardingState(FirstRunOnboardingStep.BEDTIME_GUIDANCE)
        val waiting = FirstRunOnboardingState(FirstRunOnboardingStep.WAITING_FOR_BED)

        assertEquals("bed", explanation.focusObjectId)
        assertEquals(emptySet<String>(), explanation.allowedObjectIds)
        assertEquals(setOf("bed"), waiting.allowedObjectIds)
    }
}

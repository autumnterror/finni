package github.detrig.feature.room.presentation

import github.detrig.feature.room.domain.model.FirstRunOnboardingStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirstRunOnboardingStateTest {
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
}

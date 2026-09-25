package github.detrig.feature.room.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticParentHelpPromptTest {

    @Test
    fun offerAppearsAfterOnboardingWhenHelpIsAvailableAndWasNotShown() {
        assertFalse(canShow(onboardingCompleted = false))
        assertTrue(canShow(onboardingCompleted = true))
    }

    @Test
    fun offerIsShownOnlyOnceInTheWeek() {
        assertFalse(canShow(alreadyShownInWeek = true))
    }

    @Test
    fun offerRequiresAnEmptyPiggyBankLowWalletAndNoExistingPayments() {
        assertFalse(canShow(savingsRub = 1))
        assertFalse(canShow(savingsRub = 100))
        assertFalse(canShow(availableRub = 100))
        assertFalse(canShow(debtRub = 1))
        assertFalse(canShow(hasActiveParentHelp = true))
    }

    private fun canShow(
        onboardingCompleted: Boolean = true,
        availableRub: Long = 20,
        savingsRub: Long = 0,
        debtRub: Long = 0,
        hasActiveParentHelp: Boolean = false,
        alreadyShownInWeek: Boolean = false,
    ) = shouldOfferAutomaticParentHelp(
        onboardingCompleted = onboardingCompleted,
        availableRub = availableRub,
        savingsRub = savingsRub,
        debtRub = debtRub,
        hasActiveParentHelp = hasActiveParentHelp,
        minimumRequiredBalanceRub = 100,
        alreadyShownInWeek = alreadyShownInWeek,
    )
}

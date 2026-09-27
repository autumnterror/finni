package github.detrig.feature.room.domain.model

import github.detrig.feature.economy.domain.EconomyState
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.economy.domain.ParentHelpState
import github.detrig.feature.economy.domain.PeriodicIncome
import github.detrig.feature.economy.domain.RejectionReason
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
    fun offerAllowsInsufficientSavingsButRequiresLowWalletAndNoExistingPayments() {
        assertTrue(canShow(savingsRub = 1))
        assertFalse(canShow(savingsRub = 100))
        assertFalse(canShow(availableRub = 100))
        assertFalse(canShow(debtRub = 1))
        assertFalse(canShow(hasActiveParentHelp = true))
    }

    @Test
    fun requestInProgressKeepsHelpWindowVisibleWhileBalanceStateUpdates() {
        assertTrue(shouldRetainParentHelpDialog(
            hasActiveParentHelp = false,
            isRequestingParentHelp = true,
            helpIsAvailable = false,
        ))
    }

    @Test
    fun helpWindowClosesWhenHelpIsNoLongerAvailableAndNoRequestIsRunning() {
        assertFalse(shouldRetainParentHelpDialog(
            hasActiveParentHelp = false,
            isRequestingParentHelp = false,
            helpIsAvailable = false,
        ))
        assertTrue(shouldRetainParentHelpDialog(
            hasActiveParentHelp = true,
            isRequestingParentHelp = false,
            helpIsAvailable = false,
        ))
    }

    @Test
    fun acceptedOrAlreadyActiveRequestClosesTheAutomaticOfferDialog() {
        val help = parentHelpState()

        assertTrue(shouldCloseAutomaticParentHelpDialog(
            ParentHelpRequestResult.Accepted(help, economyState()),
            hasActiveParentHelp = false,
        ))
        assertTrue(shouldCloseAutomaticParentHelpDialog(
            ParentHelpRequestResult.AlreadyActive(help),
            hasActiveParentHelp = true,
        ))
    }

    @Test
    fun rejectedRequestClosesOnlyWhenHelpWasTakenElsewhere() {
        val rejected = ParentHelpRequestResult.Rejected(RejectionReason.INVALID_AMOUNT, economyState())

        assertFalse(shouldCloseAutomaticParentHelpDialog(rejected, hasActiveParentHelp = false))
        assertTrue(shouldCloseAutomaticParentHelpDialog(rejected, hasActiveParentHelp = true))
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

    private fun economyState() = EconomyState(
        availableRub = 20,
        savingsRub = 0,
        debtRub = 0,
        periodicIncome = PeriodicIncome(amountRub = 1, periodMillis = 1, nextAtMillis = 1),
    )

    private fun parentHelpState() = ParentHelpState(
        offerId = "offer",
        receivedRub = 100,
        totalRepaymentRub = 110,
        remainingRub = 110,
        paymentsRemaining = 2,
    )
}

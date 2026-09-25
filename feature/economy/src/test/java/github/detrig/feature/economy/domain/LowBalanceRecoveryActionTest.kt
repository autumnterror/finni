package github.detrig.feature.economy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LowBalanceRecoveryActionTest {

    @Test
    fun `uses savings before asking parents when savings cover required amount`() {
        assertEquals(
            LowBalanceRecoveryAction.USE_SAVINGS,
            lowBalanceRecoveryAction(availableRub = 20, savingsRub = 100, minimumRequiredBalanceRub = 100),
        )
    }

    @Test
    fun `asks parents when savings are one ruble below required amount`() {
        assertEquals(
            LowBalanceRecoveryAction.ASK_PARENTS,
            lowBalanceRecoveryAction(availableRub = 20, savingsRub = 99, minimumRequiredBalanceRub = 100),
        )
    }

    @Test
    fun `does not request recovery when wallet already covers required amount`() {
        assertEquals(
            LowBalanceRecoveryAction.NONE,
            lowBalanceRecoveryAction(availableRub = 100, savingsRub = 0, minimumRequiredBalanceRub = 100),
        )
    }

    @Test
    fun `offers parent help when savings cannot cover the important purchase`() {
        assertEquals(
            true,
            canOfferParentHelp(
                availableRub = 99,
                savingsRub = 0,
                debtRub = 0,
                hasActiveParentHelp = false,
                minimumRequiredBalanceRub = 100,
            ),
        )
        assertEquals(true, canOfferParentHelp(20, 1, 0, false, 100))
        assertEquals(false, canOfferParentHelp(20, 100, 0, false, 100))
        assertEquals(false, canOfferParentHelp(100, 0, 0, false, 100))
        assertEquals(false, canOfferParentHelp(20, 0, 1, false, 100))
        assertEquals(false, canOfferParentHelp(20, 0, 0, true, 100))
    }
}

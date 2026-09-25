package github.detrig.feature.planning.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PlanActualOperationTest {
    @Test
    fun mapsMandatoryOptionalAndSavingsToPlanCategories() {
        val actuals = listOf(
            PlanActualOperation.Payment("shop", 1, 100, PaymentClassification.MANDATORY),
            PlanActualOperation.Payment("minigame", 1, 200, PaymentClassification.OPTIONAL),
            PlanActualOperation.SavingsContribution("piggy-bank", 1, 300),
        )

        assertEquals(
            listOf(PlanCategory.MANDATORY, PlanCategory.WANTS, PlanCategory.SAVINGS),
            actuals.map { it.planCategory },
        )
    }
}

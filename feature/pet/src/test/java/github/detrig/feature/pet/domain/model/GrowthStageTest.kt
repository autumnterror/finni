package github.detrig.feature.pet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GrowthStageTest {
    @Test
    fun stableAssetIdsAndLegacyNamesResolveToThreeStages() {
        assertEquals(GrowthStage.BABY, GrowthStage.fromAssetId("baby"))
        assertEquals(GrowthStage.TEEN, GrowthStage.fromAssetId("teen"))
        assertEquals(GrowthStage.TEEN, GrowthStage.fromAssetId("explorer"))
        assertEquals(GrowthStage.ADULT, GrowthStage.fromAssetId("adult"))
        assertEquals(GrowthStage.ADULT, GrowthStage.fromAssetId("companion"))
        assertNull(GrowthStage.fromAssetId("unknown"))
    }
}

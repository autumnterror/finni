package github.detrig.feature.pet.api

import github.detrig.feature.pet.domain.model.GrowthStage

data class PetGrowthState(
    val stage: GrowthStage,
    val stageFromLevel: GrowthStage,
    val isDebugOverride: Boolean,
)

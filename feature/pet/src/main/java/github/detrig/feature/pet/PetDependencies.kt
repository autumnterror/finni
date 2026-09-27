package github.detrig.feature.pet

import android.content.res.AssetManager
import android.content.SharedPreferences
import github.detrig.feature.pet.domain.model.GrowthStage
import kotlinx.coroutines.flow.Flow

interface PetDependencies {
    fun profilePreferences(): SharedPreferences
    fun assets(): AssetManager
    fun observeGrowthStage(): Flow<GrowthStage>
    fun allowDebugGrowthOverride(): Boolean
}

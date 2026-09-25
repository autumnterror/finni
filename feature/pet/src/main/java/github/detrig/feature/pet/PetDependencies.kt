package github.detrig.feature.pet

import android.content.res.AssetManager
import android.content.SharedPreferences
import github.detrig.feature.gamestate.api.ProgressionApi

interface PetDependencies {
    fun profilePreferences(): SharedPreferences
    fun assets(): AssetManager
    fun progressionApi(): ProgressionApi
}

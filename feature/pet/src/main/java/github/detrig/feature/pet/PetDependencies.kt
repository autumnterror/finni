package github.detrig.feature.pet

import android.content.res.AssetManager
import android.content.SharedPreferences

interface PetDependencies {
    fun profilePreferences(): SharedPreferences
    fun assets(): AssetManager
}

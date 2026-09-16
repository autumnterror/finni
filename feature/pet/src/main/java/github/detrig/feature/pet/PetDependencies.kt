package github.detrig.feature.pet

import android.content.SharedPreferences

interface PetDependencies {
    fun profilePreferences(): SharedPreferences
}

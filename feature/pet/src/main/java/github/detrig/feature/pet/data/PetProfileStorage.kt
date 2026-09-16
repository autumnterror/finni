package github.detrig.feature.pet.data

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.model.PetSpecies
import org.json.JSONObject

internal class PetProfileStorage(
    sharedPreferences: SharedPreferences,
) : SharedStorage(sharedPreferences) {

    fun readProfile(): PetProfile? {
        val payload = readString(PROFILE_KEY, null) ?: return null
        return runCatching {
            val json = JSONObject(payload)
            require(json.getInt(VERSION_KEY) == CURRENT_VERSION)
            PetProfile(
                name = json.getString(NAME_KEY),
                species = requireNotNull(PetSpecies.fromStorageKey(json.getString(SPECIES_KEY))),
                color = requireNotNull(PetColor.fromStorageKey(json.getString(COLOR_KEY))),
            )
        }.getOrNull()
    }

    fun saveProfile(profile: PetProfile) {
        val payload = JSONObject()
            .put(VERSION_KEY, CURRENT_VERSION)
            .put(NAME_KEY, profile.name)
            .put(SPECIES_KEY, profile.species.storageKey)
            .put(COLOR_KEY, profile.color.storageKey)
            .toString()
        putString(PROFILE_KEY, payload)
    }

    private companion object {
        const val PROFILE_KEY = "pet_profile"
        const val VERSION_KEY = "version"
        const val NAME_KEY = "name"
        const val SPECIES_KEY = "species"
        const val COLOR_KEY = "color"
        const val CURRENT_VERSION = 1
    }
}

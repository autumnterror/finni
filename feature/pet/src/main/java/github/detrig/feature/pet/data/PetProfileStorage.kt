package github.detrig.feature.pet.data

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.HamsterAppearance
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
                hamsterAppearance = HamsterAppearance(
                    palette = json.optString(PALETTE_KEY, HamsterAppearance.DEFAULT_PALETTE),
                    coat = json.optString(COAT_KEY, HamsterAppearance.DEFAULT_COAT),
                    fur = json.optString(FUR_KEY, HamsterAppearance.DEFAULT_FUR),
                    ears = json.optString(EARS_KEY, HamsterAppearance.DEFAULT_EARS),
                    mark = json.optString(MARK_KEY, HamsterAppearance.DEFAULT_MARK),
                    eyes = json.optString(EYES_KEY, HamsterAppearance.DEFAULT_EYES),
                ),
            )
        }.getOrNull()
    }

    fun saveProfile(profile: PetProfile) {
        val payload = JSONObject()
            .put(VERSION_KEY, CURRENT_VERSION)
            .put(NAME_KEY, profile.name)
            .put(SPECIES_KEY, profile.species.storageKey)
            .put(COLOR_KEY, profile.color.storageKey)
            .put(PALETTE_KEY, profile.hamsterAppearance.palette)
            .put(COAT_KEY, profile.hamsterAppearance.coat)
            .put(FUR_KEY, profile.hamsterAppearance.fur)
            .put(EARS_KEY, profile.hamsterAppearance.ears)
            .put(MARK_KEY, profile.hamsterAppearance.mark)
            .put(EYES_KEY, profile.hamsterAppearance.eyes)
            .toString()
        putString(PROFILE_KEY, payload)
    }

    private companion object {
        const val PROFILE_KEY = "pet_profile"
        const val VERSION_KEY = "version"
        const val NAME_KEY = "name"
        const val SPECIES_KEY = "species"
        const val COLOR_KEY = "color"
        const val PALETTE_KEY = "appearance_palette"
        const val COAT_KEY = "appearance_coat"
        const val FUR_KEY = "appearance_fur"
        const val EARS_KEY = "appearance_ears"
        const val MARK_KEY = "appearance_mark"
        const val EYES_KEY = "appearance_eyes"
        const val CURRENT_VERSION = 1
    }
}

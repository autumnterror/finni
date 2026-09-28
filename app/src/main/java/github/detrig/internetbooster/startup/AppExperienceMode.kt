package github.detrig.internetbooster.startup

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppExperienceMode {
    GAME,
    DEMONSTRATION,
}

internal class AppExperienceModeStorage(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val mutableSelectedMode = MutableStateFlow(readMode())

    val selectedMode: StateFlow<AppExperienceMode?> = mutableSelectedMode.asStateFlow()

    fun isDemoMode(): Boolean = mutableSelectedMode.value == AppExperienceMode.DEMONSTRATION

    fun select(mode: AppExperienceMode) {
        check(preferences.edit().putString(MODE_KEY, mode.name).commit()) {
            "Failed to save the selected app mode"
        }
        mutableSelectedMode.value = mode
    }

    fun clearSelection() {
        check(preferences.edit().remove(MODE_KEY).commit()) {
            "Failed to clear the selected app mode"
        }
        mutableSelectedMode.value = null
    }

    private fun readMode(): AppExperienceMode? = preferences.getString(MODE_KEY, null)
        ?.let { stored -> AppExperienceMode.values().firstOrNull { it.name == stored } }

    private companion object {
        const val PREFERENCES_NAME = "app_experience"
        const val MODE_KEY = "selected_mode"
    }
}

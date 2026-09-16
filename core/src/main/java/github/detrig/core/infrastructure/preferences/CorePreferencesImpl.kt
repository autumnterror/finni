package github.detrig.core.infrastructure.preferences

import android.content.SharedPreferences

/**
 * Реализация [CorePreferences] поверх SharedPreferences.
 */
internal class CorePreferencesImpl(
    sharedPreferences: SharedPreferences,
    private val persistentStorage: PersistentConfigStorage,
) : SharedStorage(sharedPreferences), CorePreferences {

    override fun clear() {
        super.clear()
        persistentStorage.clear()
    }

    override fun forceClear() {
        super.forceClear()
        persistentStorage.forceClear()
    }
}

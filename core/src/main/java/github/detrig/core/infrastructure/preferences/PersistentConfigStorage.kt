package github.detrig.core.infrastructure.preferences

import android.content.SharedPreferences

/**
 * Хранилище настроек, которые не должны очищаться обычным clear().
 */
internal class PersistentConfigStorage(
    sharedPreferences: SharedPreferences,
) : SharedStorage(sharedPreferences) {

    companion object {
        const val FILE_NAME = "persistent_config_storage"
    }

    override fun clear() = Unit

    override fun forceClear() = Unit
}

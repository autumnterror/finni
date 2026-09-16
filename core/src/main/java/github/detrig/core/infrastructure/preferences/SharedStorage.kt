package github.detrig.core.infrastructure.preferences

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Locale

/**
 * Базовая обертка над SharedPreferences.
 */
open class SharedStorage(
    private val sharedPreferences: SharedPreferences,
) {

    companion object {
        private const val SIZE_SUFFIX = "__SIZE"
        private const val INDEX_SUFFIX = "__%d"
    }

    open fun clear() {
        sharedPreferences.edit { clear() }
    }

    @SuppressLint("ApplySharedPref")
    open fun forceClear() {
        sharedPreferences.edit(commit = true) { clear() }
    }

    fun hasKey(key: String): Boolean = sharedPreferences.contains(key)

    fun readInt(key: String, defaultValue: Int): Int = sharedPreferences.getInt(key, defaultValue)

    fun putInt(key: String, value: Int) = sharedPreferences.edit { putInt(key, value) }

    fun readFloat(key: String, defaultValue: Float): Float = sharedPreferences.getFloat(key, defaultValue)

    fun putFloat(key: String, value: Float) = sharedPreferences.edit { putFloat(key, value) }

    fun readDouble(key: String, defaultValue: Double): Double {
        val value = sharedPreferences.getString(key, null) ?: return defaultValue
        return value.toDouble()
    }

    fun putDouble(key: String, value: Double) = sharedPreferences.edit { putString(key, value.toString()) }

    fun readLong(key: String, defaultValue: Long): Long = sharedPreferences.getLong(key, defaultValue)

    fun putLong(key: String, value: Long) = sharedPreferences.edit { putLong(key, value) }

    fun readBoolean(key: String, defaultValue: Boolean): Boolean = sharedPreferences.getBoolean(key, defaultValue)

    fun putBoolean(key: String, value: Boolean) = sharedPreferences.edit { putBoolean(key, value) }

    fun readString(key: String?, defaultValue: String = ""): String =
        checkNotNull(sharedPreferences.getString(key, defaultValue))

    @JvmName("readStringNullable")
    fun readString(key: String?, defaultValue: String?): String? = sharedPreferences.getString(key, defaultValue)

    fun putString(key: String?, value: String?) = sharedPreferences.edit { putString(key, value) }

    fun readStringSet(key: String?, value: Set<String> = emptySet()): Set<String> =
        checkNotNull(sharedPreferences.getStringSet(key, value))

    fun putStringSet(key: String?, value: Set<String>?) = sharedPreferences.edit { putStringSet(key, value) }

    fun readStringList(key: String): List<String> {
        val listSize = sharedPreferences.getInt(getListIntKey(key), 0)
        if (listSize == 0) return emptyList()
        return List(listSize) { index -> readString(getListStringKey(key, index)) }
    }

    fun saveStringList(key: String, value: List<String>?) {
        clearStringList(key)
        if (value == null) return

        sharedPreferences.edit {
            putInt(getListIntKey(key), value.size)
            repeat(value.size) { index ->
                putString(getListStringKey(key, index), value[index])
            }
        }
    }

    fun remove(key: String?) {
        if (sharedPreferences.contains(key)) {
            sharedPreferences.edit { remove(key) }
        }
    }

    private fun clearStringList(key: String) {
        val listSize = sharedPreferences.getInt(getListIntKey(key), 0)
        if (listSize == 0) return

        sharedPreferences.edit {
            remove(getListIntKey(key))
            repeat(listSize) { index ->
                remove(getListStringKey(key, index))
            }
        }
    }

    private fun getListIntKey(key: String): String = "$key$SIZE_SUFFIX"

    private fun getListStringKey(key: String, index: Int): String =
        "$key${String.format(Locale.getDefault(), INDEX_SUFFIX, index)}"
}

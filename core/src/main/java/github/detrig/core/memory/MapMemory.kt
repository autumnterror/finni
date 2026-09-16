package github.detrig.core.memory

import kotlin.reflect.KProperty

/**
 * Простое in-memory хранилище на delegate properties.
 *
 * Значения живут только в памяти процесса и очищаются через [clear].
 */
open class MapMemory {

    private val values = mutableMapOf<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T? {
        return values[property.name] as? T
    }

    operator fun <T> setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        values[property.name] = value
    }

    fun remove(key: String) {
        values.remove(key)
    }

    fun clear() {
        values.clear()
    }
}

package github.detrig.core.di

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> diDemand(createFunction: () -> T): ReadWriteProperty<Any?, T?> {
    return DiComponentFieldDemand(createFunction)
}

fun <T> diNullable(): ReadWriteProperty<Any?, T?> {
    return DiComponentFieldDemand()
}

private class DiComponentFieldDemand<T>(
    private val createFunction: (() -> T)? = null,
) : ReadWriteProperty<Any?, T?> {

    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return value ?: createFunction?.invoke()?.also { createdValue ->
            value = createdValue
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T?) {
        value = newValue
    }
}
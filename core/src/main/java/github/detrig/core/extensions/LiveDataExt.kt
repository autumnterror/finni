package github.detrig.core.extensions

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <X, Y> LiveData<X>.mapDistinct(transform: (X) -> Y): LiveData<Y> {
    return map(transform).distinctUntilChanged()
}

inline fun <X> LiveData<X>.filter(crossinline predicate: (X) -> Boolean): LiveData<X> {
    val outputLiveData = MediatorLiveData<X>()
    outputLiveData.addSource(this) { currentValue ->
        if (predicate(currentValue)) {
            outputLiveData.value = currentValue
        }
    }
    return outputLiveData
}

inline fun <reified X> LiveData<*>.filterIsInstance(): LiveData<X> {
    val outputLiveData = MediatorLiveData<X>()
    outputLiveData.addSource(this) { currentValue ->
        if (currentValue is X) {
            outputLiveData.value = currentValue
        }
    }
    return outputLiveData
}

fun <T : Any> MutableLiveData<T>.delegate(): ReadWriteProperty<Any, T> {
    return object : ReadWriteProperty<Any, T> {
        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) = onNext(value)
        override fun getValue(thisRef: Any, property: KProperty<*>): T = requireValue()
    }
}

fun <T> LiveData<T>.observeNotNull(
    owner: LifecycleOwner,
    observer: (t: T & Any) -> Unit,
) = observe(owner) {
    it?.let(observer)
}

fun <T> MutableLiveData<T>.onNext(next: T) {
    value = next
}

fun <T : Any> LiveData<T>.requireValue(): T = checkNotNull(value)

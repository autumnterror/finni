package github.detrig.core

import androidx.annotation.MainThread

interface Mediator<T> {
    @MainThread
    fun getApi(): T
}
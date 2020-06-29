package com.pandulapeter.beagle.core.manager.listener

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

internal abstract class BaseListenerManager<T> {

    private val listeners = mutableListOf<T>()
    private val internalListeners = mutableListOf<T>()

    fun addInternalListener(listener: T) {
        if (!internalListeners.contains(listener)) {
            internalListeners.add(listener)
        }
    }

    @Suppress("unused")
    fun addListener(listener: T, lifecycleOwner: LifecycleOwner?) {
        lifecycleOwner?.lifecycle?.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreate() = addListener(listener)

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                removeListener(listener)
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        }) ?: addListener(listener)
    }

    fun removeListener(listener: T) {
        this.listeners.remove(listener)
    }

    fun clearListeners() = listeners.clear()

    protected fun notifyListeners(notification: (T) -> Unit) {
        internalListeners.forEach(notification)
        listeners.forEach(notification)
    }

    @CallSuper
    protected open fun addListener(listener: T) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
}
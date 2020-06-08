package com.pandulapeter.beagle.appDemo.utils

import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.pandulapeter.beagle.appDemo.R

inline fun <reified T : Fragment> FragmentManager.handleReplace(
    tag: String = T::class.java.name,
    addToBackStack: Boolean = false,
    @IdRes containerId: Int = R.id.fragment_container,
    crossinline newInstance: () -> T
) {
    beginTransaction().apply {
        setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
        replace(containerId, findFragmentByTag(tag) ?: newInstance(), tag)
        if (addToBackStack) {
            addToBackStack(null)
        }
        setReorderingAllowed(true)
        commitAllowingStateLoss()
    }
}

inline fun <T> LiveData<T>.observe(owner: LifecycleOwner, crossinline onChanged: (T) -> Unit) = Observer<T> { t -> onChanged.invoke(t) }.also {
    observe(owner, it)
}

var View.visible
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }
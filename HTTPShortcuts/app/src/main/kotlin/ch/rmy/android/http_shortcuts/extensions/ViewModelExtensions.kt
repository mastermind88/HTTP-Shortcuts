package ch.rmy.android.http_shortcuts.extensions

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import ch.rmy.android.http_shortcuts.Application
import kotlin.properties.ReadOnlyProperty

inline fun <reified V : ViewModel> FragmentActivity.bindViewModel(): ReadOnlyProperty<FragmentActivity, V> {
    return bindViewModelOf(V::class.java)
}

inline fun <reified V : ViewModel> Fragment.bindViewModel(): ReadOnlyProperty<Fragment, V> {
    return bindViewModelOf(V::class.java)
}

fun <V : ViewModel> FragmentActivity.bindViewModelOf(clazz: Class<V>): ReadOnlyProperty<FragmentActivity, V> =
    bind(clazz, viewModelProviderFinder)

fun <V : ViewModel> Fragment.bindViewModelOf(clazz: Class<V>): ReadOnlyProperty<Fragment, V> =
    bind(clazz, viewModelProviderFinder)

private val FragmentActivity.viewModelProviderFinder: FragmentActivity.() -> ViewModelProvider
    get() = { ViewModelProviders.of(this) }

private val Fragment.viewModelProviderFinder: Fragment.() -> ViewModelProvider
    get() = { ViewModelProviders.of(activity!!) }

private fun <T, V : ViewModel> bind(clazz: Class<V>, finder: T.() -> ViewModelProvider) =
    LazyWithTarget { t: T, _ -> t.finder().get(clazz) }

@Deprecated("Avoid using context in a view model")
val AndroidViewModel.context: Context
    get() = getApplication<Application>().context

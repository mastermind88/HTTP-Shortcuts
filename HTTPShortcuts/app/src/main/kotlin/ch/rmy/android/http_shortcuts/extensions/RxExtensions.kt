package ch.rmy.android.http_shortcuts.extensions

import androidx.lifecycle.LifecycleOwner
import ch.rmy.android.http_shortcuts.exceptions.CanceledByUserException
import ch.rmy.android.http_shortcuts.utils.RxLifecycleObserver
import io.reactivex.CompletableEmitter
import io.reactivex.Observable
import io.reactivex.SingleEmitter
import io.reactivex.functions.Consumer

fun CompletableEmitter.cancel() {
    if (!isDisposed) {
        onError(CanceledByUserException())
    }
}

fun <T> SingleEmitter<T>.cancel() {
    if (!isDisposed) {
        onError(CanceledByUserException())
    }
}

fun <T : Any> Observable<T>.observe(
    lifecycleOwner: LifecycleOwner,
    onEvent: Consumer<T>,
): RxLifecycleObserver<T> =
    RxLifecycleObserver(this, onEvent).apply {
        lifecycleOwner.lifecycle.addObserver(this)
    }

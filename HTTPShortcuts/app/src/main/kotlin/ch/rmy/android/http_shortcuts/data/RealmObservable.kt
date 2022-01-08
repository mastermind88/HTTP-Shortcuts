package ch.rmy.android.http_shortcuts.data

import android.os.Looper
import ch.rmy.android.http_shortcuts.extensions.detachFromRealm
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmQuery

class RealmObservable<T : RealmObject>(
    private val realmFactory: RealmFactory,
    private val query: (RealmContext.() -> RealmQuery<T>),
) : Observable<List<T>>() {

    override fun subscribeActual(observer: Observer<in List<T>>) {
        var isDisposed = false
        var realm: Realm? = null
        var looper: Looper? = null
        observer.onSubscribe(object : Disposable {
            override fun dispose() {
                isDisposed = false
                AndroidSchedulers.from(looper ?: return).scheduleDirect {
                    realm?.close()
                }
            }

            override fun isDisposed() =
                isDisposed
        })

        looper = Looper.myLooper()
        realm = realmFactory.createRealm()
            .apply {
                registerChangeListener(createContext(), observer)
            }
    }

    private fun registerChangeListener(realmContext: RealmContext, observer: Observer<in List<T>>) {
        val results = query.invoke(realmContext).findAllAsync()
        results.addChangeListener { data ->
            data
                .takeIf { it.isValid && it.isLoaded }
                ?.detachFromRealm()
                ?.let(observer::onNext)
        }
    }
}
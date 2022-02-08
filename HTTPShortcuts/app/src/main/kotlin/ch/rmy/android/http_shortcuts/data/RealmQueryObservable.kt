package ch.rmy.android.http_shortcuts.data

import ch.rmy.android.http_shortcuts.extensions.detachFromRealm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.RealmResults
import kotlin.reflect.KFunction1

class RealmQueryObservable<T : RealmObject>(
    realmFactory: RealmFactory,
    private val query: (RealmContext.() -> RealmQuery<T>),
) : RealmObservable<T>(realmFactory) {

    // Keeping a strong reference to the list to prevent garbage collection
    private var results: RealmResults<T>? = null

    override fun registerChangeListener(realmContext: RealmContext, onDataChanged: (List<T>) -> Unit) {
        results = query.invoke(realmContext).findAllAsync()
            .apply {
                addChangeListener { data ->
                    data
                        .takeIf { it.isValid && it.isLoaded }
                        ?.detachFromRealm()
                        ?.let(onDataChanged)
                }
            }
    }

    override fun onDispose() {
        results?.removeAllChangeListeners()
        results = null
    }
}
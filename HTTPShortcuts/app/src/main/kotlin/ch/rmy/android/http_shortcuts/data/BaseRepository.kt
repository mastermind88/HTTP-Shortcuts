package ch.rmy.android.http_shortcuts.data

import ch.rmy.android.http_shortcuts.extensions.detachFromRealm
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmQuery

abstract class BaseRepository {

    protected fun <T : RealmObject> query(query: RealmContext.() -> RealmQuery<T>): Single<List<T>> =
        Single.fromCallable {
            RealmFactory.getInstance().createRealm().use { realm ->
                query(
                    object : RealmContext {
                        override val realmInstance: Realm
                            get() = realm
                    }
                )
                    .findAll()
                    .detachFromRealm()
            }
        }
            .subscribeOn(Schedulers.io())

    protected fun <T : RealmObject> queryItem(query: RealmContext.() -> RealmQuery<T>): Single<T> =
        query(query)
            .map {
                it.first()
            }

    protected fun <T : RealmObject> observe(query: RealmContext.() -> RealmQuery<T>): Observable<List<T>> =
        Observable.fromCallable {
            TODO() // TODO: How to keep Realm open?
        }

    protected fun <T : RealmObject> observeList(query: RealmContext.() -> RealmList<T>): Observable<List<T>> =
        Observable.fromCallable {
            TODO() // TODO: How to keep Realm open?
        }

    protected fun <T : RealmObject> observeItem(query: RealmContext.() -> RealmQuery<T>): Observable<T> =
        observe(query)
            .filter { it.isNotEmpty() }
            .map { it.first() }

    protected fun commitTransaction(transaction: RealmTransactionContext.() -> Unit): Completable =
        Completable.fromAction {
            RealmFactory.getInstance().createRealm().use { realm ->
                realm.executeTransaction {
                    transaction(
                        object : RealmTransactionContext {
                            override val realmInstance: Realm
                                get() = realm
                        }
                    )
                }
            }
        }
            .subscribeOn(Schedulers.single())

}
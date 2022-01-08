package ch.rmy.android.http_shortcuts.data

import io.realm.Realm
import io.realm.RealmObject

interface RealmContext {
    val realmInstance: Realm
}

fun Realm.createContext() =
    object : RealmContext {
        override val realmInstance: Realm
            get() = this@createContext
    }

fun Realm.createTransactionContext() =
    object : RealmTransactionContext {
        override val realmInstance: Realm
            get() = this@createTransactionContext
    }

interface RealmTransactionContext : RealmContext {
    fun <T : RealmObject> copy(`object`: T): T =
        realmInstance.copyToRealm(`object`)

    fun <T : RealmObject> copyOrUpdate(`object`: T): T =
        realmInstance.copyToRealmOrUpdate(`object`)
}

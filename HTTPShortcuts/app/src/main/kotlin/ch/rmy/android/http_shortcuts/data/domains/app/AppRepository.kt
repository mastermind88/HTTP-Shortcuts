package ch.rmy.android.http_shortcuts.data.domains.app

import ch.rmy.android.http_shortcuts.data.BaseRepository
import ch.rmy.android.http_shortcuts.data.domains.getAppLock
import ch.rmy.android.http_shortcuts.data.domains.getBase
import ch.rmy.android.http_shortcuts.data.models.AppLock
import ch.rmy.android.http_shortcuts.utils.Optional
import io.reactivex.Completable
import io.reactivex.Single

class AppRepository : BaseRepository() {

    fun getGlobalCode() =
        query {
            getBase()
        }
            .map {
                it.firstOrNull()
                    ?.globalCode
                    ?: ""
            }

    fun getToolbarTitle() =
        query {
            getBase()
        }
            .map {
                it.firstOrNull()
                    ?.title
                    ?: ""
            }

    fun getObservableToolbarTitle() =
        observeItem {
            getBase()
        }
            .map { base ->
                base.title?.takeUnless { it.isBlank() } ?: ""
            }

    fun setToolbarTitle(title: String) =
        commitTransaction {
            getBase()
                .findFirst()
                ?.title = title
        }

    fun setGlobalCode(globalCode: String?): Completable =
        commitTransaction {
            getBase()
                .findFirst()
                ?.let { base ->
                    base.globalCode = globalCode
                }
        }

    fun getLock(): Single<Optional<AppLock>> =
        query {
            getAppLock()
        }
            .map {
                // TODO: Throw an Exception instead when not found?
                Optional(it.firstOrNull())
            }

    fun getObservableLock() =
        observe {
            getAppLock()
        }
            .map {
                Optional(it.firstOrNull())
            }

    fun setLock(passwordHash: String) =
        commitTransaction {
            copyOrUpdate(AppLock(passwordHash))
        }

    fun removeLock() =
        commitTransaction {
            getAppLock()
                .findAll()
                .deleteAllFromRealm()
        }
}
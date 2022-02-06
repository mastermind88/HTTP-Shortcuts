package ch.rmy.android.http_shortcuts.data.domains.pending_executions

import ch.rmy.android.http_shortcuts.data.BaseRepository
import ch.rmy.android.http_shortcuts.data.domains.getPendingExecution
import ch.rmy.android.http_shortcuts.data.domains.getPendingExecutions
import ch.rmy.android.http_shortcuts.data.models.PendingExecution
import io.reactivex.Observable
import io.reactivex.Single
import java.util.Date

class PendingExecutionsRepository : BaseRepository() {

    fun getPendingExecution(id: String): Single<List<PendingExecution>> =
        query {
            getPendingExecution(id)
        }

    fun getObservablePendingExecutions(): Observable<List<PendingExecution>> =
        observe {
            getPendingExecutions()
        }

    fun createPendingExecution(
        shortcutId: String,
        resolvedVariables: Map<String, String> = emptyMap(),
        tryNumber: Int = 0,
        waitUntil: Date? = null,
        requiresNetwork: Boolean = false,
        recursionDepth: Int = 0,
    ) =
        commitTransaction {
            copy(
                PendingExecution.createNew(
                    shortcutId,
                    resolvedVariables,
                    tryNumber,
                    waitUntil,
                    requiresNetwork,
                    recursionDepth,
                )
            )
        }

    fun removePendingExecution(shortcutId: String) =
        commitTransaction {
            getPendingExecutions(shortcutId)
                .findAll()
                .deleteAllFromRealm()
        }
}
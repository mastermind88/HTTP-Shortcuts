package ch.rmy.android.http_shortcuts.data.domains.variables

import ch.rmy.android.framework.data.BaseRepository
import ch.rmy.android.framework.data.RealmTransactionContext
import ch.rmy.android.http_shortcuts.data.RealmFactory
import ch.rmy.android.http_shortcuts.data.domains.getTemporaryShortcut
import ch.rmy.android.http_shortcuts.data.domains.getTemporaryVariable
import ch.rmy.android.http_shortcuts.data.enums.ShortcutExecutionType
import ch.rmy.android.http_shortcuts.data.enums.VariableType
import ch.rmy.android.http_shortcuts.data.models.ResponseHandling
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.data.models.Variable
import io.reactivex.Completable
import io.reactivex.Observable

class TemporaryVariableRepository : BaseRepository(RealmFactory.getInstance()) {

    fun getObservableTemporaryVariable(): Observable<Variable> =
        observeItem {
            getTemporaryVariable()
        }

    fun createNewTemporaryVariable(type: VariableType): Completable =
        commitTransaction {
            copyOrUpdate(
                Variable(
                    id = Variable.TEMPORARY_ID,
                    variableType = type,
                )
            )
        }

    fun setKey(key: String): Completable =
        commitTransactionForVariable { variable ->
            variable.key = key
        }

    fun setTitle(title: String): Completable =
        commitTransactionForVariable { variable ->
            variable.title = title
        }

    fun setUrlEncode(enabled: Boolean): Completable =
        commitTransactionForVariable { variable ->
            variable.urlEncode = enabled
        }

    fun setJsonEncode(enabled: Boolean): Completable =
        commitTransactionForVariable { variable ->
            variable.jsonEncode = enabled
        }

    fun setShareText(enabled: Boolean): Completable =
        commitTransactionForVariable { variable ->
            variable.isShareText = enabled
        }

    fun setRememberValue(enabled: Boolean): Completable =
        commitTransactionForVariable { variable ->
            variable.rememberValue = enabled
        }

    fun setValue(value: String): Completable =
        commitTransactionForVariable { variable ->
            variable.value = value
        }

    fun setDataForType(data: Map<String, String?>): Completable =
        commitTransactionForVariable { variable ->
            variable.dataForType = data
        }

    private fun commitTransactionForVariable(transaction: RealmTransactionContext.(Variable) -> Unit) =
        commitTransaction {
            transaction(
                getTemporaryVariable()
                    .findFirst()
                    ?: return@commitTransaction
            )
        }
}

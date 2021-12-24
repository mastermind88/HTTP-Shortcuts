package ch.rmy.android.http_shortcuts.activities.main

import android.app.Application
import androidx.lifecycle.LiveData
import ch.rmy.android.http_shortcuts.data.domains.pending_executions.PendingExecutionsRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.widgets.WidgetsRepository
import ch.rmy.android.http_shortcuts.data.livedata.ListLiveData
import ch.rmy.android.http_shortcuts.data.models.Category
import ch.rmy.android.http_shortcuts.data.models.PendingExecution
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import io.reactivex.android.schedulers.AndroidSchedulers

class ShortcutListViewModel(application: Application) : MainViewModel(application) {

    private val shortcutRepository = ShortcutRepository()
    private val pendingExecutionsRepository = PendingExecutionsRepository()
    private val widgetsRepository = WidgetsRepository()

    var categoryId: String = ""

    var exportedShortcutId: String? = null

    fun getCategory(): LiveData<Category?> =
        getCategoryById(categoryId)
            .findFirstAsync()
            .toLiveData()

    fun getPendingShortcuts(): ListLiveData<PendingExecution> =
        getPendingExecutions()
            .findAll()
            .toLiveData()

    fun getShortcuts(): ListLiveData<Shortcut> =
        getBase()
            .findFirst()!!
            .categories
            .firstOrNull { category -> category.id == categoryId }
            ?.shortcuts
            ?.toLiveData()
            ?: (object : ListLiveData<Shortcut>() {})

    fun deleteShortcut(shortcutId: String) =
        shortcutRepository.deleteShortcut(shortcutId)
            .mergeWith(pendingExecutionsRepository.removePendingExecution(shortcutId))
            .andThen(widgetsRepository.deleteDeadWidgets())
            .observeOn(AndroidSchedulers.mainThread())

    fun removePendingExecution(shortcutId: String) =
        pendingExecutionsRepository
            .removePendingExecution(shortcutId)
            .observeOn(AndroidSchedulers.mainThread())

    fun duplicateShortcut(shortcutId: String, newName: String, newPosition: Int?, categoryId: String) =
        shortcutRepository.duplicateShortcut(shortcutId, newName, newPosition, categoryId)
}

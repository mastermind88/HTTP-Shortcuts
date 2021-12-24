package ch.rmy.android.http_shortcuts.data.domains.widgets

import ch.rmy.android.http_shortcuts.data.BaseRepository
import ch.rmy.android.http_shortcuts.data.domains.getDeadWidgets

class WidgetsRepository : BaseRepository() {

    fun deleteDeadWidgets() =
        commitTransaction {
            getDeadWidgets()
                .findAll()
                .forEach { widget ->
                    widget.deleteFromRealm()
                }
        }

}
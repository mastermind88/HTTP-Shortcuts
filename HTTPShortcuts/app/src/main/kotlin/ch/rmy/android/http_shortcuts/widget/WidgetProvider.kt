package ch.rmy.android.http_shortcuts.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, widgetIds: IntArray) {
        WidgetManager.updateWidgets(context, widgetIds.toTypedArray())
    }

    override fun onDeleted(context: Context, widgetIds: IntArray) {
        Transactions.commit { realm ->
            Repository.getDeadWidgets(realm)
                .forEach { widget ->
                    widget.deleteFromRealm()
                }
            Repository.getWidgetsByIds(realm, widgetIds.toTypedArray())
                .forEach { widget ->
                    widget.deleteFromRealm()
                }
        }
            .subscribe()
    }
}

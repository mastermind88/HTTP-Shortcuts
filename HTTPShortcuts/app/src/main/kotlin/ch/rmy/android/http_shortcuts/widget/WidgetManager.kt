package ch.rmy.android.http_shortcuts.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.ExecuteActivity
import ch.rmy.android.http_shortcuts.data.models.Widget
import ch.rmy.android.http_shortcuts.utils.IconUtil

object WidgetManager {

    fun createWidget(widgetId: Int, shortcutId: String, showLabel: Boolean, labelColor: String?) =
        Transactions.commit { realm ->
            realm.copyToRealmOrUpdate(
                Widget(
                    widgetId = widgetId,
                    shortcut = Repository.getShortcutById(realm, shortcutId),
                    showLabel = showLabel,
                    labelColor = labelColor,
                )
            )
        }

    fun updateWidgets(context: Context, widgetIds: Array<Int>) {
        if (widgetIds.isEmpty()) {
            return
        }
        Controller().use { controller ->
            controller.getWidgetsByIds(widgetIds)
                .forEach { widget ->
                    updateWidget(context, widget)
                }
        }
    }

    fun updateWidgets(context: Context, shortcutId: String) {
        Controller().use { controller ->
            controller.getWidgetsForShortcut(shortcutId)
                .forEach { widget ->
                    updateWidget(context, widget)
                }
        }
    }

    private fun updateWidget(context: Context, widget: Widget) {
        val shortcut = widget.shortcut ?: return
        RemoteViews(context.packageName, R.layout.widget).also { views ->
            views.setOnClickPendingIntent(
                R.id.widget_base,
                ExecuteActivity.IntentBuilder(context, shortcut.id)
                    .build()
                    .let { intent ->
                        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PendingIntent.FLAG_IMMUTABLE
                        } else 0
                        PendingIntent.getActivity(context, 0, intent, flags)
                    }
            )
            if (widget.showLabel) {
                views.setViewVisibility(R.id.widget_label, View.VISIBLE)
                views.setTextViewText(R.id.widget_label, shortcut.name)
                views.setTextColor(R.id.widget_label, widget.labelColor?.let(Color::parseColor) ?: Color.WHITE)
            } else {
                views.setViewVisibility(R.id.widget_label, View.GONE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                views.setImageViewIcon(R.id.widget_icon, IconUtil.getIcon(context, shortcut.icon))
            } else {
                views.setImageViewUri(R.id.widget_icon, shortcut.icon.getIconURI(context, external = true))
            }

            AppWidgetManager.getInstance(context)
                .updateAppWidget(widget.widgetId, views)
        }
    }

    fun getIntent(widgetId: Int) =
        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }

    fun getWidgetIdFromIntent(intent: Intent): Int =
        intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
}

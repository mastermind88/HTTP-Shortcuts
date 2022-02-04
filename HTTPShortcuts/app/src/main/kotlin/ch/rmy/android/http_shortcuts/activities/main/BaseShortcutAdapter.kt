package ch.rmy.android.http_shortcuts.activities.main

import ch.rmy.android.http_shortcuts.activities.BaseAdapter
import ch.rmy.android.http_shortcuts.data.models.PendingExecution
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

abstract class BaseShortcutAdapter : BaseAdapter<ShortcutItem>() {

    sealed interface UserEvent {
        data class ShortcutClicked(val id: String) : UserEvent
    }

    private val userEventSubject = PublishSubject.create<UserEvent>()

    val userEvents: Observable<UserEvent>
        get() = userEventSubject

    internal var shortcutsPendingExecution: List<PendingExecution> = emptyList()

    /*
    val emptyMarker = EmptyMarker(
        context.getString(R.string.empty_state_shortcuts),
        context.getString(R.string.empty_state_shortcuts_instructions),
    )

    protected val nameTextColor
        get() = when (textColor) {
            TextColor.BRIGHT -> color(context, R.color.text_color_primary_bright)
            TextColor.DARK -> color(context, R.color.text_color_primary_dark)
        }

    protected val descriptionTextColor
        get() = when (textColor) {
            TextColor.BRIGHT -> color(context, R.color.text_color_secondary_bright)
            TextColor.DARK -> color(context, R.color.text_color_secondary_dark)
        }
    */

}

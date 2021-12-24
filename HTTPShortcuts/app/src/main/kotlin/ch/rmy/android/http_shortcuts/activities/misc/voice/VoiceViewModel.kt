package ch.rmy.android.http_shortcuts.activities.misc.voice

import android.app.Application
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.ExecuteActivity
import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.utils.text.Localizable
import ch.rmy.android.http_shortcuts.utils.text.StringResLocalizable

class VoiceViewModel(application: Application) : BaseViewModel<Unit>(application) {

    private val shortcutRepository = ShortcutRepository()

    override fun initViewState() = Unit

    private lateinit var shortcutName: String

    fun initialize(shortcutName: String?) {
        if (shortcutName == null) {
            finish(skipAnimation = true)
            return
        }
        this.shortcutName = shortcutName
    }

    private fun showMessageDialog(message: Localizable) {
        emitEvent(
            ViewModelEvent.ShowDialog { context ->
                DialogBuilder(context)
                    .message(message)
                    .positive(R.string.dialog_ok)
                    .dismissListener {
                        onMessageDialogDismissed()
                    }
                    .showIfPossible()
            }
        )
    }

    private fun onMessageDialogDismissed() {
        finish(skipAnimation = true)
    }

    override fun onInitialized() {
        shortcutRepository.getShortcutByNameOrId(shortcutName)
            .subscribe(
                { shortcut ->
                    executeShortcut(shortcut.id)
                    finish(skipAnimation = true)
                },
                {
                    showMessageDialog(StringResLocalizable(R.string.error_shortcut_not_found_for_deep_link, shortcutName))
                }
            )
            .attachTo(destroyer)
    }

    private fun executeShortcut(shortcutId: String) {
        emitEvent(
            ViewModelEvent.OpenActivity { context ->
                ExecuteActivity.IntentBuilder(context, shortcutId)
            }
        )
    }
}

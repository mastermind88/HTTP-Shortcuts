package ch.rmy.android.http_shortcuts.activities.editor.advancedsettings

import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.utils.text.Localizable
import kotlin.time.Duration

abstract class AdvancedSettingsEvent : ViewModelEvent() {
    class ShowTimeoutDialog(val timeout: Duration, val getLabel: (Duration) -> Localizable) : AdvancedSettingsEvent()
    object ShowClientCertDialog : AdvancedSettingsEvent()
}

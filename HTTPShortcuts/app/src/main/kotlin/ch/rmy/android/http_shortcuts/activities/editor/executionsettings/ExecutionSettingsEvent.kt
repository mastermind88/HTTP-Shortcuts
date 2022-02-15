package ch.rmy.android.http_shortcuts.activities.editor.executionsettings

import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.utils.text.Localizable
import kotlin.time.Duration

abstract class ExecutionSettingsEvent : ViewModelEvent() {
    class ShowDelayDialog(val delay: Duration, val getLabel: (Duration) -> Localizable) : ExecutionSettingsEvent()
}

package ch.rmy.android.http_shortcuts.activities.editor.scripting

import ch.rmy.android.http_shortcuts.activities.ViewModelEvent

abstract class ScriptingEvent : ViewModelEvent() {
    data class ShowCodeSnippetPicker(
        val includeFileOptions: Boolean,
        val includeResponseOptions: Boolean,
        val includeNetworkErrorOption: Boolean,
        val target: Target,
    ) : ViewModelEvent() {
        enum class Target {
            PREPARE,
            SUCCESS,
            FAILURE,
        }
    }
}
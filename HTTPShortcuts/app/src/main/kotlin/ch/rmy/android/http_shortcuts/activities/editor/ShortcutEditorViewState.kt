package ch.rmy.android.http_shortcuts.activities.editor

import ch.rmy.android.http_shortcuts.data.enums.ShortcutExecutionType
import ch.rmy.android.http_shortcuts.icons.ShortcutIcon
import ch.rmy.android.http_shortcuts.utils.text.Localizable

data class ShortcutEditorViewState(
    val toolbarSubtitle: Localizable? = null,
    val shortcutExecutionType: ShortcutExecutionType = ShortcutExecutionType.APP,
    val shortcutIcon: ShortcutIcon = ShortcutIcon.NoIcon,
    val shortcutName: String = "",
    val shortcutDescription: String = "",
    val testButtonVisible: Boolean = false,
    val saveButtonVisible: Boolean = false,
    val requestBodyButtonEnabled: Boolean = false,
    val basicSettingsSubtitle: Localizable = Localizable.EMPTY,
    val headersSubtitle: Localizable = Localizable.EMPTY,
    val requestBodySubtitle: Localizable = Localizable.EMPTY,
    val requestBodySettingsSubtitle: Localizable = Localizable.EMPTY,
    val authenticationSettingsSubtitle: Localizable = Localizable.EMPTY,
    val scriptingSubtitle: Localizable = Localizable.EMPTY,
    val triggerShortcutsSubtitle: Localizable = Localizable.EMPTY,
)
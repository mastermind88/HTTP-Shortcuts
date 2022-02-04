package ch.rmy.android.http_shortcuts.activities.main

data class ShortcutItem(
    val textColor: TextColor,
) {
    enum class TextColor {
        BRIGHT,
        DARK,
    }
}
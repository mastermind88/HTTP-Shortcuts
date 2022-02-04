package ch.rmy.android.http_shortcuts.utils

enum class CategoryLayoutType(val type: String) {
    LINEAR_LIST("linear_list"),
    GRID("grid");

    override fun toString() =
        type

    companion object {
        fun parse(type: String?) =
            values().firstOrNull { it.type == type }
                ?: LINEAR_LIST
    }
}
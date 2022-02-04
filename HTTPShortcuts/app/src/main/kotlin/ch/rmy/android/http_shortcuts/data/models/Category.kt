package ch.rmy.android.http_shortcuts.data.models

import ch.rmy.android.http_shortcuts.utils.CategoryLayoutType
import ch.rmy.android.http_shortcuts.utils.UUIDUtils.isUUID
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required

open class Category(
    @Required
    var name: String = "",
) : RealmObject(), HasId {

    @PrimaryKey
    override var id: String = ""
    var shortcuts: RealmList<Shortcut> = RealmList()

    @Required
    var layoutType: String = CategoryLayoutType.LINEAR_LIST.type

    var background: String = BACKGROUND_TYPE_WHITE
    var hidden: Boolean = false

    val categoryLayoutType
        get() = CategoryLayoutType.parse(layoutType)

    fun validate() {
        if (!isUUID(id) && id.toIntOrNull() == null) {
            throw IllegalArgumentException("Invalid category ID found, must be UUID: $id")
        }

        if (CategoryLayoutType.values().none { it.type == layoutType }) {
            throw IllegalArgumentException("Invalid layout type: $layoutType")
        }

        if (background !in setOf(BACKGROUND_TYPE_WHITE, BACKGROUND_TYPE_BLACK, BACKGROUND_TYPE_WALLPAPER)) {
            throw IllegalArgumentException("Invalid background: $background")
        }

        shortcuts.forEach(Shortcut::validate)
    }

    companion object {

        const val BACKGROUND_TYPE_WHITE = "white"
        const val BACKGROUND_TYPE_BLACK = "black"
        const val BACKGROUND_TYPE_WALLPAPER = "wallpaper"
    }
}

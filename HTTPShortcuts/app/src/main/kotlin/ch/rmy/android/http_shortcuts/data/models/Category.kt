package ch.rmy.android.http_shortcuts.data.models

import ch.rmy.android.http_shortcuts.utils.CategoryBackgroundType
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

    var background: String = CategoryBackgroundType.WHITE.type
    var hidden: Boolean = false

    var categoryLayoutType
        get() = CategoryLayoutType.parse(layoutType)
        set(value) {
            layoutType = value.type
        }

    var categoryBackgroundType
        get() = CategoryBackgroundType.parse(background)
        set(value) {
            background = value.type
        }

    fun validate() {
        if (!isUUID(id) && id.toIntOrNull() == null) {
            throw IllegalArgumentException("Invalid category ID found, must be UUID: $id")
        }

        if (CategoryLayoutType.values().none { it.type == layoutType }) {
            throw IllegalArgumentException("Invalid layout type: $layoutType")
        }

        if (CategoryBackgroundType.values().none { it.type == background }) {
            throw IllegalArgumentException("Invalid background: $background")
        }

        shortcuts.forEach(Shortcut::validate)
    }
}

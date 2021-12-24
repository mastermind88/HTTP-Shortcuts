package ch.rmy.android.http_shortcuts.activities.editor.shortcuts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseAdapter
import ch.rmy.android.http_shortcuts.databinding.ListItemShortcutTriggerBinding
import ch.rmy.android.http_shortcuts.icons.ShortcutIcon
import ch.rmy.android.http_shortcuts.scripting.shortcuts.ShortcutPlaceholder
import ch.rmy.android.http_shortcuts.utils.HTMLUtil
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ShortcutsAdapter : BaseAdapter<ShortcutPlaceholder>() {

    sealed interface UserEvent {
        data class ShortcutClicked(val id: String) : UserEvent
    }

    private val userEventSubject = PublishSubject.create<UserEvent>()

    val userEvents: Observable<UserEvent>
        get() = userEventSubject

    override fun areItemsTheSame(oldItem: ShortcutPlaceholder, newItem: ShortcutPlaceholder): Boolean =
        oldItem.id == newItem.id

    override fun createViewHolder(viewType: Int, parent: ViewGroup, layoutInflater: LayoutInflater): RecyclerView.ViewHolder =
        ShortcutViewHolder(ListItemShortcutTriggerBinding.inflate(layoutInflater, parent, false))

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, position: Int, item: ShortcutPlaceholder) {
        (holder as ShortcutViewHolder).setItem(item)
    }

    inner class ShortcutViewHolder(
        private val binding: ListItemShortcutTriggerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var shortcutId: String

        init {
            binding.root.setOnClickListener {
                userEventSubject.onNext(ShortcutsAdapter.UserEvent.ShortcutClicked(shortcutId))
            }
        }

        fun setItem(shortcut: ShortcutPlaceholder) {
            this.shortcutId = shortcut.id
            if (shortcut.isDeleted()) {
                val deleted = itemView.context.getString(R.string.placeholder_deleted_shortcut)
                binding.name.text = HTMLUtil.format("<i>$deleted</i>")
                binding.icon.setIcon(ShortcutIcon.NoIcon)
            } else {
                binding.name.text = shortcut.name
                binding.icon.setIcon(shortcut.icon)
            }
        }
    }
}

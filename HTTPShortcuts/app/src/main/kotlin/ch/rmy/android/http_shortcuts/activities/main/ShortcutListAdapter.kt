package ch.rmy.android.http_shortcuts.activities.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.rmy.android.http_shortcuts.databinding.ListItemShortcutBinding
import ch.rmy.android.http_shortcuts.extensions.consume
import ch.rmy.android.http_shortcuts.extensions.context
import ch.rmy.android.http_shortcuts.extensions.visible

class ShortcutListAdapter : BaseShortcutAdapter() {

    override fun createViewHolder(parent: ViewGroup, layoutInflater: LayoutInflater) =
        ShortcutViewHolder(ListItemShortcutBinding.inflate(layoutInflater, parent, false))

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: ShortcutListItem.Shortcut) {
        (holder as ShortcutListAdapter.ShortcutViewHolder).setItem(item)
    }

    inner class ShortcutViewHolder(
        private val binding: ListItemShortcutBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var shortcutId: String

        init {
            binding.root.setOnClickListener {
                userEventSubject.onNext(UserEvent.ShortcutClicked(shortcutId))
            }
            binding.root.setOnLongClickListener {
                if (isLongClickingEnabled) {
                    consume {
                        userEventSubject.onNext(UserEvent.ShortcutLongClicked(shortcutId))
                    }
                } else {
                    false
                }
            }
        }

        fun setItem(item: ShortcutListItem.Shortcut) {
            shortcutId = item.id
            binding.name.text = item.name
            binding.description.text = item.description
            binding.description.visible = item.description.isNotEmpty()
            binding.icon.setIcon(item.icon)
            binding.waitingIcon.visible = item.isPending
            binding.name.setTextColor(getPrimaryTextColor(context, item.textColor))
            binding.description.setTextColor(getSecondaryTextColor(context, item.textColor))
        }
    }
}

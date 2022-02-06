package ch.rmy.android.http_shortcuts.activities.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.rmy.android.http_shortcuts.activities.variables.VariableAdapter
import ch.rmy.android.http_shortcuts.activities.variables.VariableListItem
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.databinding.GridItemShortcutBinding
import ch.rmy.android.http_shortcuts.databinding.ListItemVariableBinding
import ch.rmy.android.http_shortcuts.extensions.consume
import ch.rmy.android.http_shortcuts.extensions.context
import ch.rmy.android.http_shortcuts.extensions.setText

class ShortcutGridAdapter : BaseShortcutAdapter() {

    override fun createViewHolder(parent: ViewGroup, layoutInflater: LayoutInflater) =
        ShortcutViewHolder(GridItemShortcutBinding.inflate(layoutInflater, parent, false))

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: ShortcutListItem.Shortcut) {
        (holder as ShortcutViewHolder).setItem(item)
    }

    inner class ShortcutViewHolder(
        private val binding: GridItemShortcutBinding,
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
            binding.icon.setIcon(item.icon)
            binding.name.setTextColor(getPrimaryTextColor(context, item.textColor))
        }
    }
}

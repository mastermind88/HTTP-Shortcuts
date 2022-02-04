package ch.rmy.android.http_shortcuts.activities.main

import android.view.LayoutInflater
import android.view.ViewGroup
import ch.rmy.android.http_shortcuts.activities.LegacyBaseViewHolder
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.databinding.ListItemShortcutBinding
import ch.rmy.android.http_shortcuts.extensions.visible

class ShortcutListAdapter : BaseShortcutAdapter() {

    override fun createViewHolder(parentView: ViewGroup) =
        ShortcutViewHolder(ListItemShortcutBinding.inflate(LayoutInflater.from(parentView.context), parentView, false))

    inner class ShortcutViewHolder(private val binding: ListItemShortcutBinding) :
        LegacyBaseViewHolder<Shortcut>(binding.root, this@ShortcutListAdapter) {

        override fun updateViews(item: Shortcut) {
            binding.name.text = item.name
            binding.description.text = item.description
            binding.description.visible = item.description.isNotEmpty()
            binding.icon.setIcon(item.icon)
            binding.waitingIcon.visible = isPendingExecution(item.id)
            binding.name.setTextColor(nameTextColor)
            binding.description.setTextColor(descriptionTextColor)
        }

        private fun isPendingExecution(shortcutId: String) = shortcutsPendingExecution.any {
            it.shortcutId == shortcutId
        }
    }
}

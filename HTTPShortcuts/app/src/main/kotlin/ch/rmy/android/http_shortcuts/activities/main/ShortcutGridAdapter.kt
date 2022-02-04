package ch.rmy.android.http_shortcuts.activities.main

import android.view.LayoutInflater
import android.view.ViewGroup
import ch.rmy.android.http_shortcuts.activities.LegacyBaseViewHolder
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.databinding.GridItemShortcutBinding

class ShortcutGridAdapter : BaseShortcutAdapter() {

    override fun createViewHolder(parentView: ViewGroup) =
        ShortcutViewHolder(GridItemShortcutBinding.inflate(LayoutInflater.from(parentView.context), parentView, false))

    inner class ShortcutViewHolder(private val binding: GridItemShortcutBinding) :
        LegacyBaseViewHolder<Shortcut>(binding.root, this@ShortcutGridAdapter) {

        override fun updateViews(item: Shortcut) {
            binding.name.text = item.name
            binding.icon.setIcon(item.icon)
            binding.name.setTextColor(nameTextColor)
        }
    }
}

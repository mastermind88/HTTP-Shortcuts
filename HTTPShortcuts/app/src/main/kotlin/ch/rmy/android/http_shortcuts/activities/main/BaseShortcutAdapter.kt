package ch.rmy.android.http_shortcuts.activities.main

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseAdapter
import ch.rmy.android.http_shortcuts.activities.variables.VariableAdapter
import ch.rmy.android.http_shortcuts.activities.variables.VariableListItem
import ch.rmy.android.http_shortcuts.databinding.ListEmptyItemBinding
import ch.rmy.android.http_shortcuts.databinding.ListItemVariableBinding
import ch.rmy.android.http_shortcuts.extensions.color
import ch.rmy.android.http_shortcuts.extensions.setText
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

abstract class BaseShortcutAdapter : BaseAdapter<ShortcutListItem>() {

    sealed interface UserEvent {
        data class ShortcutClicked(val id: String) : UserEvent
        data class ShortcutLongClicked(val id: String) : UserEvent
    }

    protected val userEventSubject = PublishSubject.create<UserEvent>()

    var isLongClickingEnabled = false

    val userEvents: Observable<UserEvent>
        get() = userEventSubject

    override fun areItemsTheSame(oldItem: ShortcutListItem, newItem: ShortcutListItem): Boolean =
        when (oldItem) {
            is ShortcutListItem.Shortcut -> (newItem as? ShortcutListItem.Shortcut)?.id == oldItem.id
            is ShortcutListItem.EmptyState -> newItem is ShortcutListItem.EmptyState
        }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is ShortcutListItem.Shortcut -> VIEW_TYPE_SHORTCUT
            is ShortcutListItem.EmptyState -> VIEW_TYPE_EMPTY_STATE
        }

    override fun createViewHolder(viewType: Int, parent: ViewGroup, layoutInflater: LayoutInflater): RecyclerView.ViewHolder? =
        when (viewType) {
            VIEW_TYPE_SHORTCUT -> createViewHolder(parent, layoutInflater)
            VIEW_TYPE_EMPTY_STATE -> EmptyStateViewHolder(ListEmptyItemBinding.inflate(layoutInflater, parent, false))
            else -> null
        }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, position: Int, item: ShortcutListItem) {
        when (holder) {
            is EmptyStateViewHolder -> holder.setItem(item as ShortcutListItem.EmptyState)
            else -> bindViewHolder(holder, item as ShortcutListItem.Shortcut)
        }
    }

    protected abstract fun createViewHolder(parent: ViewGroup, layoutInflater: LayoutInflater): RecyclerView.ViewHolder

    protected abstract fun bindViewHolder(holder: RecyclerView.ViewHolder, item: ShortcutListItem.Shortcut)

    protected fun getPrimaryTextColor(context: Context, textColor: ShortcutListItem.TextColor) =
        when (textColor) {
            ShortcutListItem.TextColor.BRIGHT -> color(context, R.color.text_color_primary_bright)
            ShortcutListItem.TextColor.DARK -> color(context, R.color.text_color_primary_dark)
        }

    protected fun getSecondaryTextColor(context: Context, textColor: ShortcutListItem.TextColor) =
        when (textColor) {
            ShortcutListItem.TextColor.BRIGHT -> color(context, R.color.text_color_secondary_bright)
            ShortcutListItem.TextColor.DARK -> color(context, R.color.text_color_secondary_dark)
        }

    inner class EmptyStateViewHolder(
        private val binding: ListEmptyItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun setItem(item: ShortcutListItem.EmptyState) {
            binding.emptyMarker.setText(item.title)
            binding.emptyMarkerInstructions.setText(item.instructions)
        }
    }

    companion object {
        private const val VIEW_TYPE_SHORTCUT = 1
        private const val VIEW_TYPE_EMPTY_STATE = 2
    }

}

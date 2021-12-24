package ch.rmy.android.http_shortcuts.activities.variables.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.rmy.android.http_shortcuts.activities.BaseAdapter
import ch.rmy.android.http_shortcuts.data.models.Option
import ch.rmy.android.http_shortcuts.databinding.SelectOptionBinding

class SelectVariableOptionsAdapter : BaseAdapter<Option>() {

    var options: List<Option>
        get() = items
        set(value) {
            items = value
        }

    // TODO: Replace with event subject
    var clickListener: ((Option) -> Unit)? = null

    override fun createViewHolder(viewType: Int, parent: ViewGroup, layoutInflater: LayoutInflater) =
        SelectOptionViewHolder(SelectOptionBinding.inflate(layoutInflater, parent, false))

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, position: Int, item: Option) {
        (holder as SelectOptionViewHolder).updateViews(options[position])
    }

    override fun areItemsTheSame(oldItem: Option, newItem: Option) =
        oldItem.id == newItem.id

    override fun areItemContentsTheSame(oldItem: Option, newItem: Option) =
        oldItem.id == newItem.id && oldItem.isSameAs(newItem)

    inner class SelectOptionViewHolder(
        private val binding: SelectOptionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun updateViews(item: Option) {
            binding.selectOptionLabel.text = item.labelOrValue
            itemView.setOnClickListener { clickListener?.invoke(item) }
        }
    }
}

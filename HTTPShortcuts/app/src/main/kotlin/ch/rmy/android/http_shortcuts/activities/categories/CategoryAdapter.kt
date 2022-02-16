package ch.rmy.android.http_shortcuts.activities.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import ch.rmy.android.framework.extensions.context
import ch.rmy.android.framework.extensions.dimen
import ch.rmy.android.framework.extensions.setText
import ch.rmy.android.framework.extensions.visible
import ch.rmy.android.framework.ui.BaseAdapter
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.enums.CategoryLayoutType
import ch.rmy.android.http_shortcuts.databinding.ListItemCategoryBinding
import ch.rmy.android.http_shortcuts.extensions.applyTheme
import ch.rmy.android.http_shortcuts.icons.IconView
import ch.rmy.android.http_shortcuts.icons.ShortcutIcon
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CategoryAdapter : BaseAdapter<CategoryListItem>() {

    sealed interface UserEvent {
        data class CategoryClicked(val id: String) : UserEvent
    }

    private val userEventSubject = PublishSubject.create<UserEvent>()

    val userEvents: Observable<UserEvent>
        get() = userEventSubject

    override fun areItemsTheSame(oldItem: CategoryListItem, newItem: CategoryListItem): Boolean =
        oldItem.id == newItem.id

    override fun createViewHolder(viewType: Int, parent: ViewGroup, layoutInflater: LayoutInflater): RecyclerView.ViewHolder =
        CategoryViewHolder(ListItemCategoryBinding.inflate(layoutInflater, parent, false))

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, position: Int, item: CategoryListItem) {
        (holder as CategoryViewHolder).setItem(item)
    }

    inner class CategoryViewHolder(
        private val binding: ListItemCategoryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var categoryId: String

        init {
            binding.root.setOnClickListener {
                userEventSubject.onNext(UserEvent.CategoryClicked(categoryId))
            }
        }

        fun setItem(item: CategoryListItem) {
            categoryId = item.id
            binding.name.setText(item.name)
            binding.description.setText(item.description)

            updateIcons(item.icons)
            updateLayoutTypeIcon(item.layoutType)
        }

        private fun updateIcons(icons: List<ShortcutIcon>) {
            updateIconNumber(icons.size)
            icons
                .forEachIndexed { index, shortcutIcon ->
                    val iconView = binding.smallIcons.getChildAt(index) as IconView
                    iconView.setIcon(shortcutIcon)
                }
        }

        private fun updateIconNumber(number: Int) {
            val size = dimen(context, R.dimen.small_icon_size)
            while (binding.smallIcons.childCount < number) {
                val icon = IconView(context)
                icon.layoutParams = LinearLayout.LayoutParams(size, size)
                binding.smallIcons.addView(icon)
            }
            while (binding.smallIcons.childCount > number) {
                binding.smallIcons.removeViewAt(0)
            }
        }

        private fun updateLayoutTypeIcon(layoutType: CategoryLayoutType?) {
            if (layoutType == null) {
                binding.layoutTypeIcon.visible = false
            } else {
                binding.layoutTypeIcon.visible = true
                binding.layoutTypeIcon.setImageResource(
                    when (layoutType) {
                        CategoryLayoutType.GRID -> R.drawable.ic_grid
                        CategoryLayoutType.LINEAR_LIST -> R.drawable.ic_list
                    }
                )
                binding.layoutTypeIcon.applyTheme()
            }
        }
    }
}

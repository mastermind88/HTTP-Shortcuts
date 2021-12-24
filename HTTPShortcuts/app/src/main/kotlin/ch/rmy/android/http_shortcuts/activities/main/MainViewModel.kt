package ch.rmy.android.http_shortcuts.activities.main

import android.app.Application
import androidx.lifecycle.LiveData
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.data.domains.app.AppRepository
import ch.rmy.android.http_shortcuts.data.domains.categories.CategoryRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.livedata.ListLiveData
import ch.rmy.android.http_shortcuts.data.models.Category
import ch.rmy.android.http_shortcuts.extensions.toLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import org.mindrot.jbcrypt.BCrypt

open class MainViewModel(application: Application) : BaseViewModel(application) {

    private val shortcutRepository: ShortcutRepository = ShortcutRepository()
    private val categoryRepository: CategoryRepository = CategoryRepository()
    private val appRepository: AppRepository = AppRepository()

    var hasMovedToInitialCategory = false

    fun isAppLocked() =
        appRepository.getLock()
            .blockingGet()
            .value != null // FIXME

    val appLockedSource: LiveData<Boolean>
        get() = appRepository.getObservableLock()
            .map { it.value != null }
            .toLiveData(destroyer)

    fun removeAppLock(password: String): Single<Boolean> =
        appRepository.getLock()
            .flatMap { optionalLock ->
                val passwordHash = optionalLock.value?.passwordHash
                if (passwordHash != null && BCrypt.checkpw(password, passwordHash)) {
                    appRepository.removeLock()
                        .toSingleDefault(true)
                } else {
                    Single.just(passwordHash == null)
                }
            }

    fun getCategories(): ListLiveData<Category> =
        categoryRepository.getObservableCategories()
            .toLiveData(destroyer)

    fun getShortcutById(shortcutId: String) =
        try {
            shortcutRepository.getShortcutById(shortcutId)
                .blockingGet()
        } catch (e: NoSuchElementException) {
            null
        }

    fun moveShortcut(shortcutId: String, targetPosition: Int? = null, targetCategoryId: String? = null) =
        shortcutRepository.moveShortcut(shortcutId, targetPosition, targetCategoryId)
            .observeOn(AndroidSchedulers.mainThread())

    fun getToolbarTitle() =
        appRepository.getToolbarTitle()
            .blockingGet() // TODO

    fun getLiveToolbarTitle(): LiveData<String> =
        appRepository.getObservableToolbarTitle()
            .toLiveData(destroyer)

    fun setToolbarTitle(title: String) =
        appRepository.setToolbarTitle(title.trim())
            .observeOn(AndroidSchedulers.mainThread())
}

package ch.rmy.android.http_shortcuts.scripting.actions.types

import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import ch.rmy.android.framework.utils.localization.StringResLocalizable
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.execute.DialogHandle
import ch.rmy.android.http_shortcuts.activities.execute.ExecuteDialogState
import ch.rmy.android.http_shortcuts.dagger.ApplicationComponent
import ch.rmy.android.http_shortcuts.exceptions.ActionException
import ch.rmy.android.http_shortcuts.scripting.ExecutionContext
import ch.rmy.android.http_shortcuts.utils.ActivityProvider
import ch.rmy.android.http_shortcuts.utils.NetworkUtil
import ch.rmy.android.http_shortcuts.utils.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WifiSSIDAction : BaseAction() {

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var activityProvider: ActivityProvider

    @Inject
    lateinit var networkUtil: NetworkUtil

    override fun inject(applicationComponent: ApplicationComponent) {
        applicationComponent.inject(this)
    }

    override suspend fun execute(executionContext: ExecutionContext): String? {
        ensureLocationPermissionIsEnabled(activityProvider.getActivity(), executionContext.dialogHandle)
        return networkUtil.getCurrentSsid()
    }

    private suspend fun ensureLocationPermissionIsEnabled(activity: FragmentActivity, dialogHandle: DialogHandle) {
        withContext(Dispatchers.Main) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                dialogHandle.showDialog(
                    ExecuteDialogState.GenericConfirm(
                        title = StringResLocalizable(R.string.title_permission_dialog),
                        message = StringResLocalizable(R.string.message_permission_rational),
                    )
                )
            }
            requestLocationPermissionIfNeeded()
        }
    }

    private suspend fun requestLocationPermissionIfNeeded() {
        val granted = permissionManager.requestLocationPermissionIfNeeded()
        if (!granted) {
            throw ActionException {
                getString(R.string.error_failed_to_get_wifi_ssid)
            }
        }
    }
}

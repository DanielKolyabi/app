package ru.relabs.kurjer.presentation.host.featureCheckers

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import ru.relabs.kurjer.R
import ru.relabs.kurjer.utils.XiaomiUtilities
import ru.relabs.kurjer.utils.extensions.showDialog
import ru.relabs.kurjer.utils.log

class XiaomiPermissionsFeatureChecker(a: Activity) : FeatureChecker(a) {
    private var dialogShowed: Boolean = false

    override fun isFeatureEnabled(): Boolean {
        if (!XiaomiUtilities.isMIUI) return true
        val a = activity ?: return false
        return XiaomiUtilities.isCustomPermissionGranted(a, XiaomiUtilities.OP_SHOW_WHEN_LOCKED)
                && XiaomiUtilities.isCustomPermissionGranted(a, XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY)
                && XiaomiUtilities.isCustomPermissionGranted(a, XiaomiUtilities.OP_POPUPS)
    }

    override fun requestFeature() {
        if (dialogShowed) return
        activity?.let { requestActivity ->
            dialogShowed = true
            requestActivity.showDialog(
                R.string.xiaomi_permissions_request,
                R.string.settings to {
                    dialogShowed = false
                    activity?.let { dialogActivity ->
                        try {
                            dialogActivity.startActivity(XiaomiUtilities.getPermissionManagerIntent(dialogActivity))
                        } catch (x: java.lang.Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.parse("package:" + dialogActivity.packageName)
                                dialogActivity.startActivity(intent)
                            } catch (x: Exception) {
                                x.log()
                            }
                        }
                    }
                }
            )
        }
    }
}
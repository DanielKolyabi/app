package ru.relabs.kurjer.presentation.host.featureCheckers

import android.app.Activity
import android.content.Intent

import android.os.Build
import android.os.Environment

import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION

import ru.relabs.kurjer.R
import ru.relabs.kurjer.utils.extensions.showDialog
import ru.relabs.kurjer.utils.log

class ExternalStoragePermissionFC(a: Activity) : FeatureChecker(a) {
    private var dialogShowed: Boolean = false

    override fun isFeatureEnabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Environment.isExternalStorageManager()
        else
            true

    override fun requestFeature() {
        if (dialogShowed) return
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        activity?.let {
            dialogShowed = true
            it.showDialog(
                R.string.exteral_storage_permission_request,
                R.string.settings to {
                    val intent = Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    try {
                        it.startActivity(intent)
                    } catch (e: Exception) {
                        e.log()
                    }
                    dialogShowed = false
                }

            )

        }

    }


}
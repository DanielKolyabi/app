package ru.relabs.kurjer.presentation.host.featureCheckers

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ru.relabs.kurjer.R
import ru.relabs.kurjer.utils.extensions.showDialog


class PermissionFeatureChecker(a: Activity) : FeatureChecker(a) {
    private val askedPermissions = mutableMapOf<String, Boolean>()

    private var requestShowed: Boolean = false

    private val requiredPermissions = listOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.WAKE_LOCK,
        android.Manifest.permission.DISABLE_KEYGUARD ,
        


    )

    override fun isFeatureEnabled(): Boolean {
        return getNotGrantedPermissions().isEmpty()
    }

    override fun requestFeature() {
        if (requestShowed) return
        activity?.let { a ->
            val requiredPermissions = getNotGrantedPermissions()
            val rationalePermissions = requiredPermissions.filter { perm ->
                !askedPermissions.getOrPut(perm) { false } || ActivityCompat.shouldShowRequestPermissionRationale(a, perm)
            }
            val notRationalePermissions = requiredPermissions.filter { perm ->
                !ActivityCompat.shouldShowRequestPermissionRationale(a, perm)
            }
            if (rationalePermissions.isNotEmpty()) {
                requestShowed = true
                rationalePermissions.forEach {
                    askedPermissions[it] = true
                }
                ActivityCompat.requestPermissions(a, rationalePermissions.toTypedArray(), REQUEST_PERMISSIONS_CODE)
            } else {
                requestShowed = true
                a.showDialog(
                    a.resources.getString(R.string.request_permissions_rationale, getPermissionsName(notRationalePermissions)),
                    R.string.settings to {
                        requestShowed = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:" + a.packageName)
                        a.startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            requestShowed = false
            if (!isFeatureEnabled()) {
                requestFeature()
            }
        }
    }

    private fun getNotGrantedPermissions(): List<String> =
        requiredPermissions.filter { perm ->
            activity?.let { ContextCompat.checkSelfPermission(it, perm) != PackageManager.PERMISSION_GRANTED } ?: false
        }

    private fun getPermissionsName(permissions: List<String>): String =
        permissions.joinToString(separator = "\n") { getPermissionName(it) }

    private fun getPermissionName(perm: String): String = when (perm) {
        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> "Доступ ко всем файлам"
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Доступ к записи файлов"
        android.Manifest.permission.READ_EXTERNAL_STORAGE -> "Доступ к чтению файлов"
        android.Manifest.permission.ACCESS_FINE_LOCATION -> "Доступ к получению местоположения"
        android.Manifest.permission.REQUEST_INSTALL_PACKAGES -> "Разрешать устанавливать приложения"
        android.Manifest.permission.READ_PHONE_STATE -> "Доступ к информации о телефоне"
        android.Manifest.permission.WAKE_LOCK -> "Доступ к выводу устройства из сна"
        android.Manifest.permission.DISABLE_KEYGUARD -> "Доступ к запуску с экрана блокировки"
        else -> "Неизвестно"
    }

    companion object {
        const val REQUEST_PERMISSIONS_CODE = 991
    }
}
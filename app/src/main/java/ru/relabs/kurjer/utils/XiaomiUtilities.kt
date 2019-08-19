package ru.relabs.kurjer.utils

import android.annotation.TargetApi
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.annotation.SuppressLint




/**
 * Created by Daniil Kurchanov on 19.08.2019.
 */

object XiaomiUtilities {

    // custom permissions
    val OP_ACCESS_XIAOMI_ACCOUNT = 10015
    val OP_AUTO_START = 10008
    val OP_BACKGROUND_START_ACTIVITY = 10021
    val OP_BLUETOOTH_CHANGE = 10002
    val OP_BOOT_COMPLETED = 10007
    val OP_DATA_CONNECT_CHANGE = 10003
    val OP_DELETE_CALL_LOG = 10013
    val OP_DELETE_CONTACTS = 10012
    val OP_DELETE_MMS = 10011
    val OP_DELETE_SMS = 10010
    val OP_EXACT_ALARM = 10014
    val OP_GET_INSTALLED_APPS = 10022
    val OP_GET_TASKS = 10019
    val OP_INSTALL_SHORTCUT = 10017
    val OP_NFC = 10016
    val OP_NFC_CHANGE = 10009
    val OP_READ_MMS = 10005
    val OP_READ_NOTIFICATION_SMS = 10018
    val OP_SEND_MMS = 10004
    val OP_SERVICE_FOREGROUND = 10023
    val OP_SHOW_WHEN_LOCKED = 10020
    val OP_WIFI_CHANGE = 10001
    val OP_WRITE_MMS = 10006

    @SuppressLint("PrivateApi")
    fun getSystemProperty(key: String): String? {
        try {
            val props = Class.forName("android.os.SystemProperties")
            return props.getMethod("get", String::class.java).invoke(null, key) as String
        } catch (ignore: Exception) {
        }

        return null
    }

    val isMIUI: Boolean
        get() = !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"))

    val miuiMajorVersion: Int
        get() {
            val prop = getSystemProperty("ro.miui.ui.version.name")
            if (prop != null) {
                try {
                    return Integer.parseInt(prop.replace("V", ""))
                } catch (ignore: NumberFormatException) {
                }

            }
            return -1
        }

    fun getPermissionManagerIntent(appContext: Context): Intent {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        intent.putExtra("extra_package_uid", android.os.Process.myUid())
        intent.putExtra("extra_pkgname", appContext.packageName)
        return intent
    }

    @TargetApi(19)
    fun isCustomPermissionGranted(appContext: Context, permission: Int): Boolean {
        try {
            val mgr = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val m = AppOpsManager::class.java.getMethod("checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
            val result = m.invoke(mgr, permission, android.os.Process.myUid(), appContext.packageName) as Int
            return result == AppOpsManager.MODE_ALLOWED
        } catch (x: Exception) {
            x.logError()
        }

        return true
    }
}
package ru.relabs.kurjer.utils

import android.content.Context
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager


/**
 * Created by ProOrange on 05.09.2018.
 */

object NetworkHelper {
    private fun isWifiEnabled(context: Context?): Boolean {
        context ?: return false
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return false
        return wifiManager.isWifiEnabled
    }

    private fun isWifiConnected(context: Context?): Boolean {
        context ?: return false
        val wifiManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return wifiManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected ?: false
    }

    private fun isMobileDataEnabled(context: Context?): Boolean {
        context ?: return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isNetworkConnecting = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)?.isConnectedOrConnecting ?: false
        val isMobileDataEnabled = try {
            val cmClass = Class.forName(cm.javaClass.name)
            val method = cmClass.getDeclaredMethod("getMobileDataEnabled")
            method.isAccessible = true

            val status = method.invoke(cm) as Boolean

            status || isNetworkConnecting
        } catch (e: java.lang.Exception) {
            Settings.Global.getInt(context.contentResolver, "mobile_data", 0) == 1 ||
                    Settings.Secure.getInt(context.contentResolver, "mobile_data", 0) == 1 || isNetworkConnecting
        }
        return isMobileDataEnabled
    }

    fun isAirplaneModeEnabled(context: Context?): Boolean {
        context ?: return false
        return Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    }

    fun isNetworkEnabled(context: Context?): Boolean {
        return ((isWifiEnabled(context) && isWifiConnected(context)) || isMobileDataEnabled(context)) && !isAirplaneModeEnabled(context)
    }

    fun isGPSEnabled(context: Context?): Boolean {
        return ((context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.isProviderEnabled(GPS_PROVIDER)
            ?: false) && !isAirplaneModeEnabled(context)
    }

    fun isNetworkAvailable(context: Context?): Boolean {
        context ?: return false
        val status =
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo?.isConnectedOrConnecting
        return status ?: false
    }

    fun isSIMInserted(context: Context?): Boolean {
        context ?: return false
        val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (0..2).any { manager.getSimState(it) == TelephonyManager.SIM_STATE_READY }
        } else {
            manager.simState == TelephonyManager.SIM_STATE_READY
        }
    }
}
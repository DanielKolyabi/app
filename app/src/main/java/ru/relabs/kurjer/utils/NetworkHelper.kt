package ru.relabs.kurjer.utils

import android.content.Context
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


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
        return wifiManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected
    }

    private fun isMobileDataEnabled(context: Context?): Boolean {
        context ?: return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isNetworkConnecting = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting
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
        val status = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo?.isConnectedOrConnecting
        return status ?: false
    }

    fun loadUpdateFile(url: URL, onDownloadUpdate: (current: Int, total: Int) -> Unit): File {
        val file = File("test")//PathHelper.getUpdateFile()
        val connection = url.openConnection() as HttpURLConnection
        val stream = url.openStream()
        val fos = FileOutputStream(file)
        try {
            val fileSize = connection.contentLength


            val b = ByteArray(2048)

            var read = stream.read(b)
            var total = read
            while (read != -1) {
                fos.write(b, 0, read)
                read = stream.read(b)
                total += read
                Log.d("loader", "$total/$fileSize")
                onDownloadUpdate(total, fileSize)
            }
        } catch (e: Exception) {
            throw e
        } finally {
            stream.close()
            connection.disconnect()
            fos.close()
        }


        return file
    }
}
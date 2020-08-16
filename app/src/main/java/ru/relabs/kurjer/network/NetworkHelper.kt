package ru.relabs.kurjer.network

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.REQUEST_LOCATION
import ru.relabs.kurjer.files.ImageUtils
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.data.models.PhotoReportRequest
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntity
import ru.relabs.kurjer.data.database.entities.TaskItemPhotoEntity
import ru.relabs.kurjer.data.models.TaskItemReportRequest
import ru.relabs.kurjer.utils.log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


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

    fun displayLocationSettingsRequest(context: Context, activity: MainActivity) {
        val googleApiClient = GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build()
        googleApiClient.connect()

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 30000
        locationRequest.fastestInterval = 15000
        locationRequest.smallestDisplacement = 10f

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
        result.setResultCallback { result ->
            val status = result.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> Log.i("NetworkHelper", "All location settings are satisfied.")
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    Log.i("NetworkHelper", "Location settings are not satisfied. Show the user a dialog to upgrade location settings ")
                    status.startResolutionForResult(activity, REQUEST_LOCATION)
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> Log.i("NetworkHelper", "Location settings are inadequate, and cannot be fixed here. Dialog not created.")
            }
        }
    }

    fun isNetworkAvailable(context: Context?): Boolean {
        context ?: return false
        val status = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo?.isConnectedOrConnecting
        return status ?: false
    }

    private fun photoEntityToPart(partName: String, reportEnt: ReportQueryItemEntity, photoEnt: TaskItemPhotoEntity): MultipartBody.Part {
        val photoFile = PathHelper.getTaskItemPhotoFileByID(
                reportEnt.taskItemId,
                UUID.fromString(photoEnt.UUID)
        )
        if (!photoFile.exists()) {
            throw FileNotFoundException(photoFile.path)
        }

        val request =
            RequestBody.run { photoFile.asRequestBody(MediaType.run { "image/jpeg".toMediaType() }) }

        return MultipartBody.Part.createFormData(partName, photoFile.name, request)
    }

    fun loadTaskRasterizeMap(task: Task) {
        val url = URL(task.rastMapUrl)
        val bmp = BitmapFactory.decodeStream(url.openStream())
        val mapFile = PathHelper.getTaskRasterizeMapFile(task)
        ImageUtils.saveImage(bmp, mapFile)
        bmp.recycle()
    }

    fun loadUpdateFile(url: URL, onDownloadUpdate: (current: Int, total: Int) -> Unit): File {
        val file = PathHelper.getUpdateFile()
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
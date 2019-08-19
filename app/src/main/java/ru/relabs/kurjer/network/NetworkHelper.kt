package ru.relabs.kurjer.network

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import ru.relabs.kurjer.*
import ru.relabs.kurjer.files.ImageUtils
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.network.DeliveryServerAPI.api
import ru.relabs.kurjer.network.models.PhotoReportModel
import ru.relabs.kurjer.network.models.TaskItemReportModel
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity
import ru.relabs.kurjer.persistence.entities.TaskItemPhotoEntity
import ru.relabs.kurjer.utils.logError
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

    fun isNetworkEnabled(context: Context?): Boolean {
        return (isWifiEnabled(context) && isWifiConnected(context)) || isMobileDataEnabled(context)
    }

    fun isGPSEnabled(context: Context?): Boolean {
        return (context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.isProviderEnabled(GPS_PROVIDER) ?: false
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

    suspend fun sendReport(data: ReportQueryItemEntity, photos: List<TaskItemPhotoEntity>): Boolean {

        val photosMap = mutableMapOf<String, PhotoReportModel>()
        val photoParts = mutableListOf<MultipartBody.Part>()

        var imgCount = 0
        photos.forEachIndexed { i, photo ->
            try {
                photoParts.add(photoEntityToPart("img_$imgCount", data, photo))
                photosMap["img_$imgCount"] = PhotoReportModel("", photo.gps)
                imgCount++
            } catch (e: Throwable) {
                e.fillInStackTrace().logError()
            }
        }

        val reportObject = TaskItemReportModel(
                data.taskId, data.taskItemId, data.imageFolderId,
                data.gps, data.closeTime, data.userDescription, data.entrances, photosMap,
                data.batteryLevel
        )

        return api.sendTaskReport(data.taskItemId, data.token, reportObject, photoParts).await().status
    }

    private fun photoEntityToPart(partName: String, reportEnt: ReportQueryItemEntity, photoEnt: TaskItemPhotoEntity): MultipartBody.Part {
        val photoFile = PathHelper.getTaskItemPhotoFileByID(
                reportEnt.taskItemId,
                UUID.fromString(photoEnt.UUID)
        )
        if (!photoFile.exists()) {
            throw FileNotFoundException(photoFile.path)
        }
        val extension = Uri.fromFile(photoFile).toString().split(".").last()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

        val requestFile = RequestBody.create(
                MediaType.parse(mime),
                photoFile
        )
        return MultipartBody.Part.createFormData(partName, photoFile.name, requestFile)
    }

    fun loadTaskRasterizeMap(task: TaskModel, contentResolver: ContentResolver?) {
        val url = URL(task.rastMapUrl)
        val bmp = BitmapFactory.decodeStream(url.openStream())
        val mapFile = PathHelper.getTaskRasterizeMapFile(task)
        ImageUtils.saveImage(bmp, mapFile, contentResolver)
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
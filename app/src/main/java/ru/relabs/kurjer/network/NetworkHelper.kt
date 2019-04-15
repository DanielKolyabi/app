package ru.relabs.kurjer.network

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import ru.relabs.kurjer.files.ImageUtils
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.logError
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.network.DeliveryServerAPI.api
import ru.relabs.kurjer.network.models.PhotoReportModel
import ru.relabs.kurjer.network.models.TaskItemReportModel
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity
import ru.relabs.kurjer.persistence.entities.TaskItemPhotoEntity
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import android.net.NetworkInfo



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

        return Settings.Secure.getInt(context.contentResolver, "mobile_data", 0) == 1 || cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting
    }

    fun isNetworkEnabled(context: Context?): Boolean{
        return (isWifiEnabled(context) && isWifiConnected(context)) || isMobileDataEnabled(context)
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
                e.logError()
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
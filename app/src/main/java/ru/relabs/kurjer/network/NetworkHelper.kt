package ru.relabs.kurjer.network

import android.content.ContentResolver
import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.support.v4.app.Fragment
import android.util.Log
import android.webkit.MimeTypeMap
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.network.DeliveryServerAPI.api
import ru.relabs.kurjer.network.models.PhotoReportModel
import ru.relabs.kurjer.network.models.TaskItemReportModel
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity
import ru.relabs.kurjer.persistence.entities.TaskItemPhotoEntity
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * Created by ProOrange on 05.09.2018.
 */

object NetworkHelper {
    fun isNetworkAvailable(context: Context?): Boolean {
        context ?: return false
        val status = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo?.isConnectedOrConnecting
        return status ?: false
    }

    suspend fun sendReport(data: ReportQueryItemEntity, photos: List<TaskItemPhotoEntity>): Boolean {

        val photosMap = mutableMapOf<String, PhotoReportModel>()
        val photoParts = mutableListOf<MultipartBody.Part>()

        photos.forEachIndexed { i, photo ->
            photoParts.add(photoEntityToPart("img_$i", data, photo))
            photosMap["img_$i"] = PhotoReportModel("", photo.gps)
        }

        val reportObject = TaskItemReportModel(
                data.gps, data.closeTime, data.userDescription, data.entrances, photosMap
        )

        return api.sendTaskReport(data.taskItemId, data.token, reportObject, photoParts).await().status!!
    }

    private fun photoEntityToPart(partName: String, reportEnt: ReportQueryItemEntity, photoEnt: TaskItemPhotoEntity): MultipartBody.Part {
        val photoFile = PathHelper.getTaskItemPhotoFileByID(
                reportEnt.taskItemId,
                UUID.fromString(photoEnt.UUID)
        )
        val extension = Uri.fromFile(photoFile).toString().split(".").last()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

        val requestFile = RequestBody.create(
                MediaType.parse(mime),
                photoFile
        )
        return MultipartBody.Part.createFormData(partName, photoFile.name, requestFile);
    }
}
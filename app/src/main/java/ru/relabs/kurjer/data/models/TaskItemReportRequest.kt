package ru.relabs.kurjer.data.models

import com.google.gson.annotations.SerializedName
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntranceData
import ru.relabs.kurjer.models.GPSCoordinatesModel
import java.util.*

/**
 * Created by ProOrange on 07.09.2018.
 */

data class TaskItemReportRequest(
    @SerializedName("task_id") val taskId: Int,
    @SerializedName("task_item_id") val taskItemId: Int,
    @SerializedName("image_folder_id") val imageFolderId: Int,
    @SerializedName("gps") val gps: GPSCoordinatesModel,
    @SerializedName("close_time") val closeTime: Date,
    @SerializedName("description") val userDescription: String,
    @SerializedName("entrances") val entrances: List<ReportQueryItemEntranceData>,
    @SerializedName("photos") val photos: Map<String, PhotoReportRequest>,
    @SerializedName("battery_level") val batteryLevel: Int,
    @SerializedName("close_distance") val closeDistance: Int,
    @SerializedName("allowed_distance") val allowedDistance: Int,
    @SerializedName("radius_required") val radiusRequired: Boolean,
    @SerializedName("is_rejected") val isRejected: Boolean,
    @SerializedName("reject_reason") val rejectReason: String,
    @SerializedName("delivery_type") val deliveryType: Int,
    @SerializedName("is_photo_required") val isPhotoRequired: Boolean,
)

data class PhotoReportRequest(
    @SerializedName("hash") val hash: String,
    @SerializedName("gps") val gps: GPSCoordinatesModel,
    @SerializedName("entranceNumber") val entranceNumber: Int
)
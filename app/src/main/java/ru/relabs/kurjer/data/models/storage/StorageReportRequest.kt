package ru.relabs.kurjer.data.models.storage

import com.google.gson.annotations.SerializedName
import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import java.util.*

data class StorageReportRequest(
    @SerializedName("task_id")
    val taskId: Int,
    @SerializedName("token")
    val token: String,
    @SerializedName("storage_id")
    val storageId: Int,
    @SerializedName("gps")
    val gps: GPSCoordinatesModel,
    @SerializedName("description")
    val description: String,
    @SerializedName("close_time")
    val closeTime: Date,
    @SerializedName("battery_level")
    val batteryLevel: Int,
    @SerializedName("device_radius")
    val deviceRadius: Int,
    @SerializedName("device_close_any_distance")
    val deviceCloseAnyDistance: Boolean,
    @SerializedName("device_allowed_distance")
    val deviceAllowedDistance: Int,
    @SerializedName("is_photo_required")
    val isPhotoRequired: Boolean,
    @SerializedName("photos")
    val photos: Map<String, StorageReportPhotoRequest>
)

data class StorageReportPhotoRequest(
    @SerializedName("hash")
    val hash: String,
    @SerializedName("gps")
    val gps: GPSCoordinatesModel,
    @SerializedName("photo_time")
    val photoTime: Date
)

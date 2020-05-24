package ru.relabs.kurjer.network.models

import android.arch.persistence.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import ru.relabs.kurjer.models.GPSCoordinatesModel
import java.util.*

/**
 * Created by ProOrange on 07.09.2018.
 */

data class TaskItemReportModel(
        @SerializedName("task_id")
        var taskId: Int,
        @SerializedName("task_item_id")
        var taskItemId: Int,
        @SerializedName("image_folder_id")
        var imageFolderId: Int,
        var gps: GPSCoordinatesModel?,
        @SerializedName("close_time")
        var closeTime: Date,
        @SerializedName("description")
        var userDescription: String,
        var entrances: List<Pair<Int, Int>>,
        var photos: Map<String, PhotoReportModel>,
        @SerializedName("battery_level")
        var batteryLevel: Int,
        @SerializedName("close_distance")
        var closeDistance: Int,
        @SerializedName("allowed_distance")
        var allowedDistance: Int,
        @SerializedName("radius_required")
        var radiusRequired: Boolean
)

data class PhotoReportModel(
        val hash: String,
        val gps: GPSCoordinatesModel,
        val entranceNumber: Int
)
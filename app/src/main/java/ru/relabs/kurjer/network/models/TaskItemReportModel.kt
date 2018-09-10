package ru.relabs.kurjer.network.models

import com.google.gson.annotations.SerializedName
import ru.relabs.kurjer.models.GPSCoordinatesModel
import java.util.*

/**
 * Created by ProOrange on 07.09.2018.
 */

data class TaskItemReportModel(
        var gps: GPSCoordinatesModel?,
        @SerializedName("close_time")
        var closeTime: Date,
        @SerializedName("description")
        var userDescription: String,
        var entrances: List<Pair<Int, Int>>,
        var photos: Map<String, PhotoReportModel>
) {}

data class PhotoReportModel(
    val hash: String,
    val gps: GPSCoordinatesModel
)
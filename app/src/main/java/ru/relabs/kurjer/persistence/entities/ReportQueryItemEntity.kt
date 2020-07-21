package ru.relabs.kurjer.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.relabs.kurjer.models.GPSCoordinatesModel
import java.util.*

/**
 * Created by ProOrange on 06.09.2018.
 */

@Entity(tableName = "report_query")
data class ReportQueryItemEntity(
        @PrimaryKey(autoGenerate = true)
        var id: Int,
        @ColumnInfo(name = "task_item_id")
        var taskItemId: Int,
        @ColumnInfo(name = "task_id")
        var taskId: Int,
        @ColumnInfo(name = "image_folder_id")
        var imageFolderId: Int,
        var gps: GPSCoordinatesModel?,
        @ColumnInfo(name = "close_time")
        var closeTime: Date,
        @ColumnInfo(name = "user_description")
        var userDescription: String,
        var entrances: List<Pair<Int, Int>>,
        var token: String,
        @ColumnInfo(name = "battery_level")
        var batteryLevel: Int,
        @ColumnInfo(name = "remove_after_send")
        var removeAfterSend: Boolean,
        @ColumnInfo(name = "close_distance")
        var closeDistance: Int,
        @ColumnInfo(name = "allowed_distance")
        var allowedDistance: Int,
        @ColumnInfo(name = "radius_required")
        var radiusRequired: Boolean
)
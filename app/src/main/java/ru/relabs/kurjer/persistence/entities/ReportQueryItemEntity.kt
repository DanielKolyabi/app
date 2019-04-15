package ru.relabs.kurjer.persistence.entities

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
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
        var batteryLevel: Int
)
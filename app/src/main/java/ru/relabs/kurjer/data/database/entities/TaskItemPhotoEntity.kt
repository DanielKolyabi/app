package ru.relabs.kurjer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import java.util.Date

/**
 * Created by ProOrange on 03.09.2018.
 */

@Entity(tableName = "task_item_photos")

data class TaskItemPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    @ColumnInfo(name = "uuid")
    var UUID: String,
    var gps: GPSCoordinatesModel,
    @ColumnInfo(name = "task_item_id")
    var taskItemId: Int,
    @ColumnInfo(name = "entrance_number")
    var entranceNumber: Int,
    @ColumnInfo(name = "photo_date")
    var date: Date
)
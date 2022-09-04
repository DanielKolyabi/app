package ru.relabs.kurjer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.models.GPSCoordinatesModel
import java.util.*

/**
 * Created by ProOrange on 03.09.2018.
 */
@Entity(
    tableName = "task_item_results", foreignKeys = [ForeignKey(
        entity = TaskItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_item_id"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class TaskItemResultEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    @ColumnInfo(name = "task_item_id")
    var taskItemId: Int,
    var gps: GPSCoordinatesModel,
    @ColumnInfo(name = "close_time")
    var closeTime: Date?,
    var description: String,
    @ColumnInfo(name = "is_photo_required")
    var isPhotoRequired: Boolean,
) {

    companion object {
        fun empty(taskItemId: TaskItemId, isPhotoRequired: Boolean) = TaskItemResultEntity(
            0, taskItemId.id, GPSCoordinatesModel(0.0, 0.0, Date(0)), null, "", isPhotoRequired
        )
    }
}
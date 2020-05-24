package ru.relabs.kurjer.persistence.entities

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.models.TaskItemPhotoModel
import ru.relabs.kurjer.persistence.AppDatabase

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
        var entranceNumber: Int
){
    fun toTaskItemPhotoModel(db: AppDatabase): TaskItemPhotoModel? {
        val taskItemModel = db.taskItemDao().getById(taskItemId)?.toTaskItemModel(db)
        taskItemModel ?: return null
        return TaskItemPhotoModel(id, UUID, taskItemModel, gps, entranceNumber)
    }
}
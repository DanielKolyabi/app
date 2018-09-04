package ru.relabs.kurjer.persistence.entities

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import android.net.Uri
import ru.relabs.kurjer.files.PathHelper.getTaskItemPhotoFile
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskItemPhotoModel
import ru.relabs.kurjer.persistence.AppDatabase

/**
 * Created by ProOrange on 03.09.2018.
 */

@Entity(tableName = "task_item_photos", foreignKeys = [ForeignKey(
        entity = TaskItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_item_id"]
)])

data class TaskItemPhotoEntity(
        @PrimaryKey(autoGenerate = true)
        var id: Int,
        @ColumnInfo(name = "uuid")
        var UUID: String,
        var gps: GPSCoordinatesModel,
        @ColumnInfo(name = "task_item_id")
        var taskId: Int
){
    fun toTaskItemPhotoModel(db: AppDatabase): TaskItemPhotoModel {
        return TaskItemPhotoModel(id, UUID, db.taskItemDao().getById(taskId).toTaskItemModel(db), gps)
    }
}
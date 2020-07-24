package ru.relabs.kurjer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.models.TaskItemResultModel
import ru.relabs.kurjer.data.database.AppDatabase
import java.util.*

/**
 * Created by ProOrange on 03.09.2018.
 */
@Entity(tableName = "task_item_results", foreignKeys = [ForeignKey(
        entity = TaskItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_item_id"],
        onDelete = ForeignKey.CASCADE
)])

data class TaskItemResultEntity(
        @PrimaryKey(autoGenerate = true)
        var id: Int,
        @ColumnInfo(name = "task_item_id")
        var taskItemId: Int,
        var gps: GPSCoordinatesModel,
        @ColumnInfo(name = "close_time")
        var closeTime: Date?,
        var description: String
) {
    fun toTaskItemResultModel(db: AppDatabase): TaskItemResultModel {
        return TaskItemResultModel(
                id,
                db.taskItemDao().getById(taskItemId)!!.toTaskItemModel(db),
                gps, closeTime, description,
                db.entrancesDao().getByTaskItemResultId(id).map { it.toTaskItemResultEntranceModel() }
        )
    }
}
package ru.relabs.kurjer.persistence.entities

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import ru.relabs.kurjer.models.TaskItemResultEntranceModel
import ru.relabs.kurjer.persistence.AppDatabase

/**
 * Created by ProOrange on 03.09.2018.
 */
@Entity(tableName = "task_item_result_entrances", foreignKeys = [ForeignKey(
        entity = TaskItemResultEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_item_result_id"]
)])

data class TaskItemResultEntranceEntity(
        @PrimaryKey(autoGenerate = true)
        var id: Int,
        @ColumnInfo(name = "task_item_result_id")
        var taskItemResultId: Int,
        var entrance: Int,
        var state: Int
) {
    fun toTaskItemResultEntranceModel(db: AppDatabase): TaskItemResultEntranceModel {
        return TaskItemResultEntranceModel(
                id,
                db.taskItemResultsDao().getById(taskItemResultId).toTaskItemResultModel(db),
                entrance, state
        )
    }
}
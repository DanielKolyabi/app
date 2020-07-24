package ru.relabs.kurjer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.relabs.kurjer.models.TaskItemResultEntranceModel

/**
 * Created by ProOrange on 03.09.2018.
 */
@Entity(tableName = "task_item_result_entrances", foreignKeys = [ForeignKey(
        entity = TaskItemResultEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_item_result_id"],
        onDelete = ForeignKey.CASCADE
)])

data class TaskItemResultEntranceEntity(
        @PrimaryKey(autoGenerate = true)
        var id: Int,
        @ColumnInfo(name = "task_item_result_id")
        var taskItemResultId: Int,
        var entrance: Int,
        var state: Int
) {
    fun toTaskItemResultEntranceModel(): TaskItemResultEntranceModel {
        return TaskItemResultEntranceModel(
                id,
                entrance, state
        )
    }
}
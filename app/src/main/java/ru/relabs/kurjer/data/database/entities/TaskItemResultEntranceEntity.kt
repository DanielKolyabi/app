package ru.relabs.kurjer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.relabs.kurjer.domain.models.EntranceNumber
import ru.relabs.kurjer.domain.models.TaskItemResultId

/**
 * Created by ProOrange on 03.09.2018.
 */
@Entity(
    tableName = "task_item_result_entrances", foreignKeys = [ForeignKey(
        entity = TaskItemResultEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_item_result_id"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class TaskItemResultEntranceEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    @ColumnInfo(name = "task_item_result_id")
    var taskItemResultId: Int,
    var entrance: Int,
    var state: Int,
    @ColumnInfo(name = "user_description")
    var userDescription: String,
    @ColumnInfo(name = "is_photo_required")
    var isPhotoRequired: Boolean,
) {
    companion object {
        fun empty(taskItemResultId: TaskItemResultId, entrance: EntranceNumber, isPhotoRequired: Boolean) = TaskItemResultEntranceEntity(
            0, taskItemResultId.id, entrance.number, 0, "", isPhotoRequired
        )
    }
}
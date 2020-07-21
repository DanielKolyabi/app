package ru.relabs.kurjer.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import ru.relabs.kurjer.models.EntranceModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.persistence.AppDatabase

/**
 * Created by ProOrange on 31.08.2018.
 */

@Entity(tableName = "task_items", foreignKeys = [ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_id"],
        onDelete = CASCADE
)])

data class TaskItemEntity(
        @ColumnInfo(name = "address_id")
        var addressId: Int,
        var state: Int,
        @PrimaryKey
        var id: Int,
        var notes: List<String>,
        var entrances: List<Int>,
        var subarea: Int,
        var bypass: Int,
        var copies: Int,
        @ColumnInfo(name = "task_id")
        var taskId: Int,
        @ColumnInfo(name = "need_photo")
        var needPhoto: Boolean
) {
    fun toTaskItemModel(db: AppDatabase): TaskItemModel {
        return TaskItemModel(
                db.addressDao().getById(addressId)!!.toAddressModel(),
                state, id, notes, entrances.map { EntranceModel(it, false) }, subarea, bypass, copies, needPhoto,
                db.entranceDataDao().getAllForTaskItem(id).map { it.toEntranceDataModel() }
        )
    }
}
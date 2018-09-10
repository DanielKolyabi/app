package ru.relabs.kurjer.persistence.entities

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.ForeignKey.CASCADE
import android.arch.persistence.room.PrimaryKey
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
        var taskId: Int
){
        fun toTaskItemModel(db: AppDatabase): TaskItemModel{
                return TaskItemModel(
                        db.addressDao().getById(addressId)!!.toAddressModel(),
                        state, id, notes, entrances, subarea, bypass, copies
                )
        }
}
package ru.relabs.kurjer.persistence.entities

import android.arch.persistence.room.*
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.persistence.AppDatabase
import java.util.*

/**
 * Created by ProOrange on 31.08.2018.
 */

@Entity(tableName = "task_items", foreignKeys = [ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_id"]
), ForeignKey(
        entity = AddressEntity::class,
        parentColumns = ["id"],
        childColumns = ["address_id"]
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
                        db.addressDao().getById(addressId).toAddressModel(),
                        state, id, notes, entrances, subarea, bypass, copies
                )
        }
}
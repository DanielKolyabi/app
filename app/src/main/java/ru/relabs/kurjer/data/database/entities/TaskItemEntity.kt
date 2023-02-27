package ru.relabs.kurjer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Created by ProOrange on 31.08.2018.
 */

@Entity(
    tableName = "task_items", foreignKeys = [ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_id"],
        onDelete = CASCADE
    )]
)

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
    var needPhoto: Boolean,
    @ColumnInfo(name = "is_firm")
    var isFirm: Boolean,
    @ColumnInfo(name = "office_name")
    var officeName: String,
    @ColumnInfo(name = "firm_name")
    val firmName: String,
    @ColumnInfo(name = "close_radius")
    val closeRadius: Int,
    @ColumnInfo(name = "close_time")
    val closeTime: Date?
) {
    companion object {
        const val STATE_CLOSED = 1
        const val STATE_CREATED = 0
    }
}
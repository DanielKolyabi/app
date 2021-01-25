package ru.relabs.kurjer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.domain.models.TaskState
import java.util.*

/**
 * Created by ProOrange on 31.08.2018.
 */

@Entity(tableName = "tasks")
data class TaskEntity(
        @PrimaryKey
        var id: Int,
        var name: String,
        var edition: Int,
        var copies: Int,
        var packs: Int,
        var remain: Int,
        var area: Int,
        var state: Int,
        @ColumnInfo(name = "start_time")
        var startTime: Date,
        @ColumnInfo(name = "end_time")
        var endTime: Date,
        var brigade: Int,
        var brigadier: String,
        @ColumnInfo(name = "rast_map_url")
        var rastMapUrl: String,
        @ColumnInfo(name = "user_id")
        var userId: Int,
        var city: String,
        @ColumnInfo(name = "storage_address")
        var storageAddress: String,
        var iteration: Int,
        @ColumnInfo(name = "couple_type")
        var coupleType: Int,
        @ColumnInfo(name = "by_other_user")
        var byOtherUser: Boolean,
        @ColumnInfo(name = "delivery_type")
        var deliveryType: Int
) {
    val plainState
        get() = if(state and TaskModel.BY_OTHER_USER == 1){
            state xor TaskModel.BY_OTHER_USER
        }else{
            state
        }
}
package ru.relabs.kurjer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

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
    var iteration: Int,
    @ColumnInfo(name = "couple_type")
    var coupleType: Int,
    @ColumnInfo(name = "by_other_user")
    var byOtherUser: Boolean,
    @ColumnInfo(name = "delivery_type")
    var deliveryType: Int,
    var listSort: String,
    var districtType: Int,
    var orderNumber: Int,
    @ColumnInfo(name = "edition_photo_url")
    var editionPhotoUrl: String?,
    @Embedded
    var storage: StorageEntity,
    @ColumnInfo(name = "storage_close_first_required")
    var storageCloseFirstRequired: Boolean
)

data class StorageEntity(
    @ColumnInfo(name = "storage_address")
    var address: String,
    @ColumnInfo(name = "storage_lat")
    var lat: Float,
    @ColumnInfo(name = "storage_long")
    var long2: Float,
    @ColumnInfo(name = "storage_close_distance")
    var closeDistance: Int,
    @Embedded
    var closes: StorageClosesEntity,
    @ColumnInfo(name = "storage_photo_required")
    var photoRequired: Boolean,
    @ColumnInfo(name = "storage_requirements_update_date")
    var requirementsUpdateDate: Date,
    @ColumnInfo(name = "storage_description")
    var description: String
)

data class StorageClosesEntity(
    @ColumnInfo(name = "task_id")
    var taskId: Int,
    @ColumnInfo(name = "storage_id")
    var storageId: Int,
    @ColumnInfo(name = "close_date")
    var closeDate: Date
)

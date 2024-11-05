package ru.relabs.kurjer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import java.util.*

/**
 * Created by ProOrange on 06.09.2018.
 */

@Entity(tableName = "report_query")
data class ReportQueryItemEntity(
    @PrimaryKey(autoGenerate = true) var id: Int,
    @ColumnInfo(name = "task_item_id") var taskItemId: Int,
    @ColumnInfo(name = "task_id") var taskId: Int,
    @ColumnInfo(name = "image_folder_id") var imageFolderId: Int,
    @ColumnInfo(name = "gps") var gps: GPSCoordinatesModel,
    @ColumnInfo(name = "close_time") var closeTime: Date,
    @ColumnInfo(name = "user_description") var userDescription: String,
    @ColumnInfo(name = "entrances") var entrances: List<ReportQueryItemEntranceData>,
    @ColumnInfo(name = "token") var token: String,
    @ColumnInfo(name = "battery_level") var batteryLevel: Int,
    @ColumnInfo(name = "remove_after_send") var removeAfterSend: Boolean,
    @ColumnInfo(name = "close_distance") var closeDistance: Int,
    @ColumnInfo(name = "allowed_distance") var allowedDistance: Int,
    @ColumnInfo(name = "radius_required") var radiusRequired: Boolean,
    @ColumnInfo(name = "is_rejected") var isRejected: Boolean,
    @ColumnInfo(name = "reject_reason") var rejectReason: String,
    @ColumnInfo(name = "delivery_type") var deliveryType: Int,
    @ColumnInfo(name = "is_photo_required") var isPhotoRequired: Boolean,

)

data class ReportQueryItemEntranceData(
    @SerializedName("entrance") val entrance: Int,
    @SerializedName("selection") val selection: Int,
    @SerializedName("description") val description: String,
    @SerializedName("is_photo_required") val isPhotoRequired: Boolean,
)
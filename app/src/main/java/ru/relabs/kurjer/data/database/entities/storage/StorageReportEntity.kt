package ru.relabs.kurjer.data.database.entities.storage

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import java.util.*

@Entity(tableName = "storage_reports")
data class StorageReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "storage_id")
    val storageId: Int,
    @ColumnInfo(name = "task_ids")
    val taskIds: List<Int>,
    val gps: GPSCoordinatesModel,
    val description: String,
    @ColumnInfo(name = "is_closed")
    val isClosed: Boolean,
    @Embedded
    val closeData: ReportCloseDataEntity?
)

data class ReportCloseDataEntity(
    @ColumnInfo(name = "close_time")
    val closeTime: Date,
    @ColumnInfo(name = "battery_level")
    val batteryLevel: Int,
    @ColumnInfo(name = "device_radius")
    val deviceRadius: Int,
    @ColumnInfo(name = "device_close_any_distance")
    val deviceCloseAnyDistance: Boolean,
    @ColumnInfo(name = "device_allowed_distance")
    val deviceAllowedDistance: Int,
    @ColumnInfo(name = "is_photo_required")
    val isPhotoRequired: Boolean
)

package ru.relabs.kurjer.data.database.entities.storage

import androidx.room.ColumnInfo
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
    @ColumnInfo(name = "close_time")
    val closeTime: Date?,
    val gps: GPSCoordinatesModel,
    val description: String,
    @ColumnInfo(name = "is_closed")
    val isClosed: Boolean
)

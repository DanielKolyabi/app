package ru.relabs.kurjer.data.database.entities.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import ru.relabs.kurjer.domain.models.storage.ReportId
import ru.relabs.kurjer.domain.models.storage.StoragePhotoId
import java.util.*

@Entity(tableName = "storage_report_photos")
data class StorageReportPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "uuid")
    val UUID: String,
    @ColumnInfo(name = "report_id")
    val reportId: Int,
    val gps: GPSCoordinatesModel,
    val time: Date
)
package ru.relabs.kurjer.data.database.entities.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_report_query")
data class StorageReportRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "storage_report_id")
    val storageReportId: Int,
    @ColumnInfo(name = "task_id")
    val taskId: Int,
    val token: String,
)


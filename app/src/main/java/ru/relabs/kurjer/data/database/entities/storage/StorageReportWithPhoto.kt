package ru.relabs.kurjer.data.database.entities.storage

import androidx.room.Embedded
import androidx.room.Relation

data class StorageReportWithPhoto(
    @Embedded
    val storageReport: StorageReportEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "report_id"
    )
    val storageReportPhoto: List<StorageReportPhotoEntity>
)

package ru.relabs.kurjer.domain.repositories

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.storage.StorageReportEntity
import ru.relabs.kurjer.data.database.entities.storage.StorageReportPhotoEntity
import ru.relabs.kurjer.domain.mappers.database.StorageReportMapper
import ru.relabs.kurjer.domain.mappers.database.StorageReportPhotoMapper
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.models.storage.StorageReport
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import ru.relabs.kurjer.domain.models.storage.StorageReportPhoto
import ru.relabs.kurjer.utils.CustomLog
import java.util.*

class StorageRepository(db: AppDatabase) {

    private val reportDao = db.storageReportDao()
    private val photoDao = db.storagePhotoDao()

    suspend fun getReportsByStorageId(storageId: StorageId): List<StorageReport>? =
        withContext(Dispatchers.IO) {
            reportDao.getOpenedByStorageId(storageId.id, false)
                ?.map { StorageReportMapper.fromEntity(it) }
        }

    suspend fun getPhotosByReportId(storageReportId: StorageReportId): List<StorageReportPhoto> =
        withContext(Dispatchers.IO) {
            photoDao.getByStorageReportId(storageReportId.id)
                .map { StorageReportPhotoMapper.fromEntity(it) }
        }

    suspend fun createNewReport(tasks: List<Task>) {
        withContext(Dispatchers.IO) {
            val task = tasks.first()
            reportDao.insert(
                StorageReportEntity(
                    id = 0,
                    storageId = task.storage.id.id,
                    taskIds = tasks.map { it.id.id },
                    closeTime = null,
                    gps = GPSCoordinatesModel(0.0, 0.0, Date(0)),
                    description = "",
                    isClosed = false
                )
            )
        }
    }

    suspend fun savePhoto(
        reportId: StorageReportId,
        uuid: UUID,
        location: Location?
    ): StorageReportPhoto = withContext(Dispatchers.IO) {
        val gps = GPSCoordinatesModel(
            location?.latitude ?: 0.0,
            location?.longitude ?: 0.0,
            location?.time?.let { Date(it) } ?: Date(0)
        )

        val photoEntity = StorageReportPhotoEntity(0, uuid.toString(), reportId.id, gps, Date())
        val id = photoDao.insert(photoEntity)
        StorageReportPhotoMapper.fromEntity(photoEntity.copy(id = id.toInt()))
    }

    fun deletePhotoById(id: Int) {
        photoDao.deleteById(id)
    }

    fun updateReport(report: StorageReport) {
        reportDao.update(StorageReportMapper.toEntity(report))
    }

    fun getReportById(id: StorageReportId): StorageReport {
        return StorageReportMapper.fromEntity(reportDao.getById(id.id))
    }


}
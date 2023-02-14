package ru.relabs.kurjer.domain.repositories

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.storage.StorageReportEntity
import ru.relabs.kurjer.data.database.entities.storage.StorageReportPhotoEntity
import ru.relabs.kurjer.data.database.entities.storage.StorageReportRequestEntity
import ru.relabs.kurjer.domain.mappers.database.StorageReportMapper
import ru.relabs.kurjer.domain.mappers.database.StorageReportPhotoMapper
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.models.storage.StorageReport
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import ru.relabs.kurjer.domain.models.storage.StorageReportPhoto
import ru.relabs.kurjer.domain.providers.PathsProvider
import java.util.*

class StorageRepository(db: AppDatabase, private val pathsProvider: PathsProvider) {

    private val reportDao = db.storageReportDao()
    private val photoDao = db.storagePhotoDao()
    private val reportRequestDao = db.storageReportRequestDao()

    suspend fun getOpenedReportsByStorageId(storageId: StorageId): List<StorageReport>? =
        withContext(Dispatchers.IO) {
            reportDao.getOpenedByStorageId(storageId.id, false)
                ?.map { StorageReportMapper.fromEntity(it) }
        }

    suspend fun getPhotosByReportId(storageReportId: StorageReportId): List<StorageReportPhoto> =
        withContext(Dispatchers.IO) {
            photoDao.getByStorageReportId(storageReportId.id)
                .map { StorageReportPhotoMapper.fromEntity(it) }
        }

    suspend fun createNewReport(storageId: StorageId, taskIds: List<TaskId>): Int =
        withContext(Dispatchers.IO) {
            reportDao.insert(
                StorageReportEntity(
                    id = 0,
                    storageId = storageId.id,
                    taskIds = taskIds.map { it.id },
                    gps = GPSCoordinatesModel(0.0, 0.0, Date(0)),
                    description = "",
                    isClosed = false,
                    closeData = null
                )
            ).toInt()
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

    suspend fun deletePhotoById(id: Int) {
        withContext(Dispatchers.IO) {
            photoDao.deleteById(id)
        }
    }

    suspend fun updateReport(report: StorageReport) {
        withContext(Dispatchers.IO) {
            reportDao.update(StorageReportMapper.toEntity(report))
        }
    }

    suspend fun getReportById(id: StorageReportId): StorageReport {
        return withContext(Dispatchers.IO) { StorageReportMapper.fromEntity(reportDao.getById(id.id)) }
    }

    suspend fun createReportRequest(
        storageReportId: StorageReportId,
        taskId: TaskId,
        token: String
    ) {
        withContext(Dispatchers.IO) {
            reportRequestDao.insert(
                StorageReportRequestEntity(
                    0,
                    storageReportId.id,
                    taskId.id,
                    token
                )
            )
        }
    }

    suspend fun deleteReports(reports: List<StorageReport>) {
        withContext(Dispatchers.IO) {
            reportDao.deleteList(reports.map { StorageReportMapper.toEntity(it) })
        }

    }

    suspend fun getNextQuery(): StorageReportRequestEntity? = withContext(Dispatchers.IO) {
        reportRequestDao.all.firstOrNull()
    }

    suspend fun removeStorageReportQuery(storageReportQuery: StorageReportRequestEntity) {
        withContext(Dispatchers.IO) {
            reportRequestDao.delete(storageReportQuery)
        }
    }

    suspend fun isReportHasQuery(storageReportId: Int): Boolean = withContext(Dispatchers.IO) {
        reportRequestDao.getByReportId(storageReportId).isNotEmpty()
    }

    suspend fun deletePhotosReportById(storageReportId: Int) {
        withContext(Dispatchers.IO) {
            val photos = photoDao.getByStorageReportId(storageReportId)
            photos.forEach {
                val file = pathsProvider.getStoragePhotoFileById(
                    it.reportId,
                    UUID.fromString(it.UUID)
                )
                file.delete()
                photoDao.deleteById(it.id)
            }

        }
    }

    suspend fun deleteReport(storageReportId: Int) {
        withContext(Dispatchers.IO) {
            reportDao.deleteById(storageReportId)
        }
    }

}
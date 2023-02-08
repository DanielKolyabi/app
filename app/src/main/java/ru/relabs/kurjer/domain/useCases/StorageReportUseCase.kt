package ru.relabs.kurjer.domain.useCases

import android.location.Location
import androidx.core.net.toUri
import ru.relabs.kurjer.domain.models.StorageId
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import ru.relabs.kurjer.domain.models.storage.StorageReport
import ru.relabs.kurjer.domain.models.storage.StorageReportPhoto
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.repositories.StorageRepository
import ru.relabs.kurjer.presentation.storageReport.StoragePhotoWithUri
import ru.relabs.kurjer.utils.CustomLog
import java.io.File
import java.util.*

class StorageReportUseCase(
    private val storageRepository: StorageRepository,
    private val pathsProvider: PathsProvider
) {

    suspend fun getReportsByStorageId(storageId: StorageId): List<StorageReport>? =
        storageRepository.getReportsByStorageId(storageId)

    suspend fun getPhotosWithUriByReportId(storageReportId: StorageReportId): List<StoragePhotoWithUri> =
        storageRepository.getPhotosByReportId(storageReportId).map {
            StoragePhotoWithUri(
                it,
                pathsProvider.getStoragePhotoFileById(storageReportId.id, UUID.fromString(it.uuid))
                    .toUri()
            )
        }

    suspend fun getStoragePhotoFile(id: StorageReportId, uuid: UUID): File {
        return pathsProvider.getStoragePhotoFileById(id.id, uuid)
    }

    suspend fun createNewStorageReport(tasks: List<Task>) {
        storageRepository.createNewReport(tasks)
    }

    suspend fun savePhoto(
        reportId: StorageReportId,
        uuid: UUID,
        location: Location?
    ): StoragePhotoWithUri {
        val photo = storageRepository.savePhoto(reportId, uuid, location)
        val uri = pathsProvider.getStoragePhotoFileById(reportId.id, uuid).toUri()
        return StoragePhotoWithUri(photo, uri)
    }

    suspend fun removePhoto(removedPhoto: StorageReportPhoto) {
        CustomLog.writeToFile("Remove photo srid=${removedPhoto.storageReportId.id} ti=${removedPhoto.uuid}")
        val file = pathsProvider.getStoragePhotoFileById(
            removedPhoto.storageReportId.id,
            UUID.fromString(removedPhoto.uuid)
        )
        file.delete()
        storageRepository.deletePhotoById(removedPhoto.id.id)
    }

}
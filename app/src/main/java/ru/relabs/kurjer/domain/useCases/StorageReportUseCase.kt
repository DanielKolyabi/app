package ru.relabs.kurjer.domain.useCases

import android.location.Location
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import ru.relabs.kurjer.domain.models.GPSCoordinatesModel
import ru.relabs.kurjer.domain.models.StorageClosure
import ru.relabs.kurjer.domain.models.StorageId
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.domain.models.photo.StorageReportPhoto
import ru.relabs.kurjer.domain.models.storage.ReportCloseData
import ru.relabs.kurjer.domain.models.storage.StorageReport
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import ru.relabs.kurjer.domain.providers.LocationProvider
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.SettingsRepository
import ru.relabs.kurjer.domain.repositories.StorageRepository
import ru.relabs.kurjer.domain.repositories.TaskRepository
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.presentation.storageReport.StoragePhotoWithUri
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.awaitFirst
import ru.relabs.kurjer.utils.calculateDistance
import java.io.File
import java.util.Date
import java.util.UUID
import kotlin.math.roundToInt

class StorageReportUseCase(
    private val storageRepository: StorageRepository,
    private val pathsProvider: PathsProvider,
    private val pauseRepository: PauseRepository,
    private val locationProvider: LocationProvider,
    private val settingsRepository: SettingsRepository,
    private val tokenStorage: AuthTokenStorage,
    private val taskRepository: TaskRepository
) {
    suspend fun getOpenedByStorageIdWithTaskIds(storageId: StorageId, taskIds: List<TaskId>): List<StorageReport>? =
        storageRepository.getOpenedByStorageIdWithTaskIds(storageId, taskIds)

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

    suspend fun createNewStorageReport(storageId: StorageId, taskIds: List<TaskId>): StorageReport {
        val id = storageRepository.createNewReport(storageId, taskIds)
        return storageRepository.getReportById(StorageReportId(id))
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
        val file = pathsProvider.getStoragePhotoFileById(
            removedPhoto.storageReportId.id,
            UUID.fromString(removedPhoto.uuid)
        )
        file.delete()
        storageRepository.deletePhotoById(removedPhoto.id.id)
    }

    suspend fun updateReport(report: StorageReport): StorageReport = withContext(Dispatchers.IO) {
        storageRepository.updateReport(report)
        storageRepository.getReportById(report.id)
    }

    fun checkPause(): Boolean = pauseRepository.isPaused

    fun getLastLocation() = locationProvider.lastReceivedLocation()

    suspend fun loadNewLocation(scope: CoroutineScope) {
        withContext(scope.coroutineContext) {
            val delayJob = async { delay(settingsRepository.closeGpsUpdateTime.close * 1000L) }
            val gpsJob = async(Dispatchers.Default) {
                locationProvider.updatesChannel().apply {
                    receive()
                    CustomLog.writeToFile("GPS LOG: Received new location")
                    cancel()
                }
            }
            listOf(delayJob, gpsJob).awaitFirst()
            delayJob.cancel()
            gpsJob.cancel()
            CustomLog.writeToFile("GPS LOG: Got force coordinates")
        }
    }

    fun getCloseRadiusRequirement(): Boolean = settingsRepository.isStorageCloseRadiusRequired
    suspend fun stopPause() {
        if (pauseRepository.isPaused) {
            pauseRepository.stopPause(withNotify = true)
        }
    }

    suspend fun createStorageReportRequests(
        report: StorageReport,
        location: Location?,
        batteryLevel: Float,
        storage: Task.Storage,
        withClose: Boolean
    ) {
        val distance = location?.let {
            calculateDistance(
                location.latitude,
                location.longitude,
                storage.lat.toDouble(),
                storage.long.toDouble()
            )
        } ?: Int.MAX_VALUE.toDouble()

        val updatedReport = report.copy(
            gps = getReportLocation(location),
            closeData = ReportCloseData(
                Date(),
                (batteryLevel * 100).roundToInt(),
                distance.toInt(),
                !settingsRepository.isStorageCloseRadiusRequired,
                storage.closeDistance,
                storage.photoRequired
            )
        )
        if (withClose) {
            val tasks = taskRepository.getTasksByIds(report.taskIds)
            storageRepository.updateReport(updatedReport, true)
            updatedReport.taskIds.forEach { taskId ->
                storageRepository.createReportRequest(report.id, taskId, tokenStorage.getToken() ?: "")
                val storageClose = StorageClosure(taskId, storage.id, Date())
                val task = tasks.single { it.id == taskId }
                val newStorage = task.storage.copy(closes = task.storage.closes + storageClose)
                taskRepository.updateTask(task.copy(storage = newStorage, state = task.state.copy(state = TaskState.STARTED)))
                val openedReports = storageRepository.getOpenedByStorageIdWithTaskIds(storage.id, tasks.map { it.id })
                if (!openedReports.isNullOrEmpty()) {
                    storageRepository.deleteReports(openedReports)
                }
            }
        } else {
            storageRepository.updateReport(updatedReport)
            updatedReport.taskIds.forEach { taskId ->
                storageRepository.createReportRequest(report.id, taskId, tokenStorage.getToken() ?: "")
            }
        }
    }

    private fun getReportLocation(location: Location?) = when (location) {
        null -> GPSCoordinatesModel(0.0, 0.0, Date(0))
        else -> GPSCoordinatesModel(location.latitude, location.longitude, Date(location.time))
    }

    suspend fun isReportActuallyRequired(task: Task): Boolean {
        val dbTask = taskRepository.getTask(task.id) ?: return false

        val storageCloses = dbTask.storage.closes.sortedByDescending { it.closeDate }
        val isStorageCloseNotExist =
            storageCloses.isEmpty() || storageCloses.first().closeDate < settingsRepository.closeLimit
        val isStorageCloseOptional =
            dbTask.items.any {
                (it is TaskItem.Common &&
                        it.closeTime != null &&
                        (it.closeTime > settingsRepository.closeLimit || DateTime.parse(it.closeTime.toString()).dayOfYear == DateTime.now().dayOfYear))
            }
                    && dbTask.storage.requirementsUpdateDate > settingsRepository.closeLimit
        return if (isStorageCloseOptional) {
            false
        } else {
            dbTask.storageCloseFirstRequired && isStorageCloseNotExist
        }
    }
}
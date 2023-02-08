package ru.relabs.kurjer.presentation.storageReport

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import ru.relabs.kurjer.domain.models.storage.StorageReportPhoto
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.utils.*
import ru.relabs.kurjer.utils.extensions.isLocationExpired
import java.io.File
import java.util.*


object StorageReportEffects {

    fun effectLoadData(taskIds: List<TaskId>): StorageReportEffect = { c, s ->
        messages.send(StorageReportMessages.msgAddLoaders(1))
        effectLoadTasks(taskIds)
        effectLoadReport()
        effectLoadPhotos()
        messages.send(StorageReportMessages.msgAddLoaders(1))
    }

    private fun effectLoadPhotos(): StorageReportEffect = { c, s ->
        val photos =
            s.storageReport?.id?.let { c.storageReportUseCase.getPhotosWithUriByReportId(it) }
        if (photos != null) {
            messages.send(StorageReportMessages.msgPhotosLoaded(photos))
        }
    }

    private fun effectLoadTasks(taskIds: List<TaskId>): StorageReportEffect = { c, s ->
        val tasks = c.taskUseCase.getTasksByIds(taskIds)
        messages.send(StorageReportMessages.msgTasksLoaded(tasks))
    }

    fun effectNavigateBack(): StorageReportEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.router.exit()
        }
    }

    private fun effectLoadReport(): StorageReportEffect = { c, s ->
        val storageId = s.tasks.first().storage.id
        val reports = c.storageReportUseCase.getReportsByStorageId(storageId)
        if (reports != null) {
            messages.send(StorageReportMessages.msgReportLoaded(reports.first()))
        }
    }

    fun effectValidateReportExistenceAnd(msgFactory: () -> StorageReportMessage): StorageReportEffect =
        { c, s ->
            if (s.storageReport == null) {
                messages.send(StorageReportMessages.msgAddLoaders(1))
                c.storageReportUseCase.createNewStorageReport(s.tasks)
                effectLoadReport()
                messages.send(StorageReportMessages.msgAddLoaders(1))
            }
            messages.send(msgFactory())
        }

    fun effectValidateRadiusAndRequestPhoto(): StorageReportEffect = { c, s ->
        val id = s.storageReport?.id ?: StorageReportId(0)
        CustomLog.writeToFile("Request photo (${id.id}) with raidus validation")
        effectValidatePhotoRadiusAnd({ msgEffect(effectRequestPhoto(id)) }, false)
    }

    private fun effectRequestPhoto(id: StorageReportId): StorageReportEffect = { c, s ->
        when (s.storageReport) {
            null -> c.showError("re:100", true)
            else -> {
                val photoUUID = UUID.randomUUID()
                CustomLog.writeToFile("Request photo ${id.id} ${photoUUID}")
                val photoFile = c.storageReportUseCase.getStoragePhotoFile(id, photoUUID)
                withContext(Dispatchers.Main) {
                    c.requestPhoto(id, photoFile, photoUUID)
                }
            }
        }
    }


    private fun effectValidatePhotoRadiusAnd(
        msgFactory: () -> StorageReportMessage,
        withAnyRadiusWarning: Boolean,
        withLocationLoading: Boolean = true
    ): StorageReportEffect = { c, s ->
        messages.send(StorageReportMessages.msgAddLoaders(1))
        when (s.storageReport) {
            null -> c.showError("re:106", true)
            else -> {
                val storage = s.tasks.first().storage
                val location = c.locationProvider.lastReceivedLocation()
                val distance = location?.let {
                    calculateDistance(
                        location.latitude,
                        location.longitude,
                        storage.lat.toDouble(),
                        storage.long.toDouble()
                    )
                } ?: Int.MAX_VALUE.toDouble()

                val locationNotValid = location == null || Date(location.time).isLocationExpired()
                CustomLog.writeToFile(
                    "Validate photo radius (valid: ${!locationNotValid}): " +
                            "${location?.latitude}, ${location?.longitude}, ${location?.time}, " +
                            "photoRadiusRequired: ${c.settingsRepository.isStoragePhotoRadiusRequired}, " +
                            "allowedDistance: ${storage.closeDistance}, " +
                            "distance: $distance, " +
                            "storage: ${storage.id.id}"
                )

                if (locationNotValid && withLocationLoading) {
                    coroutineScope {
                        messages.send(StorageReportMessages.msgAddLoaders(1))
                        messages.send(StorageReportMessages.msgGPSLoading(true))
                        val delayJob =
                            async { delay(c.settingsRepository.closeGpsUpdateTime.photo * 1000L) }
                        val gpsJob = async(Dispatchers.Default) {
                            c.locationProvider.updatesChannel().apply {
                                receive()
                                cancel()
                            }
                        }
                        listOf(delayJob, gpsJob).awaitFirst()
                        listOf(delayJob, gpsJob).forEach {
                            if (it.isActive) {
                                it.cancel()
                            }
                        }
                        messages.send(StorageReportMessages.msgGPSLoading(false))
                        messages.send(StorageReportMessages.msgAddLoaders(-1))
                        messages.send(
                            msgEffect(
                                effectValidatePhotoRadiusAnd(
                                    msgFactory,
                                    withAnyRadiusWarning,
                                    false
                                )
                            )
                        )
                    }
                } else {
                    if (!c.settingsRepository.isStoragePhotoRadiusRequired) {
                        //https://git.relabs.ru/kurier/app/-/issues/87 если юзер может делать фото и закрывать дома вне радиуса - ему нужно показывать диалог (он админ).
                        //Если же он может только делать фото, ему о диалоге знать не надо, что бы не особо пользовался этим
                        val shouldSuppressDialog = c.settingsRepository.isStorageCloseRadiusRequired

                        if (distance > storage.closeDistance && withAnyRadiusWarning && !shouldSuppressDialog) {
                            withContext(Dispatchers.Main) {
                                c.showCloseError(
                                    R.string.storage_report_close_location_far_warning,
                                    false,
                                    null,
                                    null,
                                    emptyArray()
                                )
                            }
                        }
                        messages.send(msgFactory())
                    } else {
                        when {
                            locationNotValid -> withContext(Dispatchers.Main) {
                                c.showCloseError(
                                    R.string.report_close_location_null_error,
                                    false,
                                    null,
                                    null,
                                    emptyArray()
                                )
                            }
                            distance > storage.closeDistance -> withContext(Dispatchers.Main) {
                                c.showCloseError(
                                    R.string.storage_close_location_far_error,
                                    false,
                                    null,
                                    null,
                                    emptyArray()
                                )
                            }
                            else ->
                                messages.send(msgFactory())

                        }
                    }
                }
            }
        }
        messages.send(StorageReportMessages.msgAddLoaders(-1))
    }

    fun effectShowPhotoError(errorCode: Int): StorageReportEffect = { c, s ->
        c.showError("Не удалось сделать фотографию re:photo:$errorCode", false)
    }

    fun effectValidateRadiusAndSavePhoto(
        storageReportId: StorageReportId,
        photoUri: Uri,
        targetFile: File,
        uuid: UUID
    ): StorageReportEffect = { c, s ->
        CustomLog.writeToFile("Save photo ($uuid) with radius validation")
        effectValidatePhotoRadiusAnd({
            msgEffect(
                effectSavePhotoFromFile(
                    storageReportId,
                    photoUri,
                    targetFile,
                    uuid
                )
            )
        }, true)
    }

    private fun effectSavePhotoFromFile(
        storageReportId: StorageReportId,
        photoUri: Uri,
        targetFile: File,
        uuid: UUID
    ): StorageReportEffect = { c, s ->
        val contentResolver = c.contentResolver()
        if (contentResolver == null) {
            messages.send(msgEffect(effectShowPhotoError(8)))
        } else {
            val bmp = BitmapFactory.decodeStream(contentResolver.openInputStream(photoUri))
            if (bmp == null) {
                CustomLog.writeToFile("Photo creation failed. Uri: ${photoUri}, File: ${targetFile.path}")
                messages.send(msgEffect(effectShowPhotoError(7)))
            } else {
                effectSavePhotoFromBitmap(storageReportId, bmp, targetFile, uuid)(c, s)
            }
        }
    }

    private fun effectSavePhotoFromBitmap(
        storageReportId: StorageReportId,
        bitmap: Bitmap,
        targetFile: File,
        uuid: UUID
    ): StorageReportEffect = { c, s ->
        CustomLog.writeToFile("Save photo ${storageReportId.id} ${uuid}")
        when (val report = s.storageReport) {
            null -> c.showError("re:102", true)
            else -> {
                when (savePhotoFromBitmapToFile(bitmap, targetFile)) {
                    is Left -> messages.send(StorageReportMessages.msgPhotoError(6))
                    is Right -> {
                        val location = c.locationProvider.lastReceivedLocation()
                        val photo = c.storageReportUseCase.savePhoto(report.id, uuid, location)
                        messages.send(StorageReportMessages.msgNewPhoto(photo))
                    }
                }
            }
        }
    }

    private fun savePhotoFromBitmapToFile(
        bitmap: Bitmap,
        targetFile: File
    ): Either<Exception, File> = Either.of {
        val resized = ImageUtils.resizeBitmap(bitmap, 1024f, 768f)
        bitmap.recycle()
        ImageUtils.saveImage(resized, targetFile)
        targetFile
    }

    fun effectRemovePhoto(removedPhoto: StorageReportPhoto): StorageReportEffect = { c, s ->
        c.storageReportUseCase.removePhoto(removedPhoto)
    }

    fun effectUpdateDescription(text: String): StorageReportEffect = { c, s ->
        when (val report = s.storageReport) {
            null -> c.showError("re:103", true)
            else -> {
                val updated = c.storageReportUseCase.updateReport(report.copy(description = text))
                messages.send(StorageReportMessages.msgSavedReportLoaded(updated))
            }
        }
    }
}






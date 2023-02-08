package ru.relabs.kurjer.presentation.storageReport

import android.net.Uri
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.storage.StorageReport
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import ru.relabs.kurjer.domain.models.storage.StorageReportPhoto
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgState
import ru.relabs.kurjer.utils.CustomLog
import java.io.File
import java.util.*

object StorageReportMessages {
    fun msgInit(taskIds: List<TaskId>): StorageReportMessage = msgEffects({
        it
    },
        {
            listOf(
                StorageReportEffects.effectLoadData(taskIds)
            )
        }
    )

    fun msgNavigateBack(): StorageReportMessage = msgEffect(
        StorageReportEffects.effectNavigateBack()
    )

    fun msgTasksLoaded(tasks: List<Task>): StorageReportMessage =
        msgState { it.copy(tasks = tasks) }

    fun msgAddLoaders(i: Int): StorageReportMessage =
        msgState { it.copy(loaders = it.loaders + i) }

    fun msgReportLoaded(report: StorageReport): StorageReportMessage = msgState {
        it.copy(storageReport = report)
    }

    fun msgPhotosLoaded(photos: List<StoragePhotoWithUri>): StorageReportMessage = msgState {
        it.copy(storagePhotos = photos)
    }

    fun msgPhotoClicked(): StorageReportMessage = msgEffect(
        StorageReportEffects.effectValidateReportExistenceAnd { msgEffect(StorageReportEffects.effectValidateRadiusAndRequestPhoto()) }
    )

    fun msgRemovePhotoClicked(removedPhoto: StorageReportPhoto): StorageReportMessage =
        msgEffects({ state -> state.copy(storagePhotos = state.storagePhotos.filter { photo -> photo.photo != removedPhoto }) },{
            listOf(StorageReportEffects.effectRemovePhoto(removedPhoto))
        })


    fun msgGPSLoading(enabled: Boolean): StorageReportMessage =
        msgState { it.copy(isGPSLoading = enabled) }


    fun msgPhotoError(errorCode: Int): StorageReportMessage = msgEffect(
        StorageReportEffects.effectShowPhotoError(errorCode)
    )

    fun msgPhotoCaptured(
        storageReportId: StorageReportId,
        photoUri: Uri,
        targetFile: File,
        uuid: UUID
    ): StorageReportMessage = msgEffect(
        StorageReportEffects.effectValidateRadiusAndSavePhoto(
            storageReportId,
            photoUri,
            targetFile,
            uuid
        )
    )

    fun msgNewPhoto(newPhoto: StoragePhotoWithUri): StorageReportMessage = msgState {
        CustomLog.writeToFile("New photo added to list ${newPhoto.photo.uuid}")
        it.copy(storagePhotos = it.storagePhotos + listOf(newPhoto))
    }
}
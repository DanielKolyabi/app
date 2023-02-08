package ru.relabs.kurjer.presentation.storageReport

import android.content.ContentResolver
import android.location.Location
import android.net.Uri
import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.storage.StorageReport
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import ru.relabs.kurjer.domain.models.storage.StorageReportPhoto
import ru.relabs.kurjer.domain.providers.LocationProvider
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.SettingsRepository
import ru.relabs.kurjer.domain.useCases.StorageReportUseCase
import ru.relabs.kurjer.domain.useCases.TaskUseCase
import ru.relabs.kurjer.presentation.base.tea.*
import java.io.File
import java.util.*

data class StorageReportState(
    var tasks: List<Task> = listOf(),
    var storageReport: StorageReport? = null,
    var storagePhotos: List<StoragePhotoWithUri> = listOf(),
    var loaders: Int = 0,
    var isGPSLoading: Boolean = false
)

data class StoragePhotoWithUri(val photo: StorageReportPhoto, val uri: Uri)

class StorageReportContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {
    val taskUseCase: TaskUseCase by inject()
    val storageReportUseCase: StorageReportUseCase by inject()
    val locationProvider: LocationProvider by inject()
    val pauseRepository: PauseRepository by inject()
    val settingsRepository: SettingsRepository by inject()

    var showError: suspend (code: String, isFatal: Boolean) -> Unit = { _, _ -> }
    var showCloseError: (msgRes: Int, showNext: Boolean, location: Location?, rejectReason: String?, msgFormat: Array<Any>) -> Unit = { _, _, _, _, _ -> }
    var requestPhoto: (id: StorageReportId, targetFile: File, uuid: UUID) -> Unit = { _, _, _ -> }
    var contentResolver: () -> ContentResolver? = { null }
    var showPausedWarning: () -> Unit = {}
    var showPhotosWarning: () -> Unit = {}
    var showPreCloseDialog: (location: Location?, rejectReason: String?) -> Unit = { _, _ -> }

}

typealias StorageReportMessage = ElmMessage<StorageReportContext, StorageReportState>
typealias StorageReportEffect = ElmEffect<StorageReportContext, StorageReportState>
typealias StorageReportRender = ElmRender<StorageReportState>
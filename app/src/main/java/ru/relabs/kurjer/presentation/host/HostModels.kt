package ru.relabs.kurjer.presentation.host

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.domain.controllers.ServiceEventController
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.models.AppUpdate
import ru.relabs.kurjer.domain.models.AppUpdatesInfo
import ru.relabs.kurjer.domain.providers.DeviceUUIDProvider
import ru.relabs.kurjer.domain.providers.LocationProvider
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.PauseType
import ru.relabs.kurjer.domain.repositories.SettingsRepository
import ru.relabs.kurjer.domain.storage.CurrentUserStorage
import ru.relabs.kurjer.domain.useCases.AppUpdateUseCase
import ru.relabs.kurjer.presentation.base.fragment.AppBarSettings
import ru.relabs.kurjer.presentation.base.tea.*
import ru.relabs.kurjer.presentation.host.featureCheckers.FeatureCheckersContainer
import java.io.File

/**
 * Created by Daniil Kurchanov on 20.11.2019.
 */
data class HostState(
    val settings: AppBarSettings = AppBarSettings(),
    val loaders: Int = 0,
    val userLogin: UserLogin? = null,

    val updateLoadProgress: Int? = null,
    val appUpdates: AppUpdatesInfo? = null,
    val isUpdateLoadingFailed: Boolean = false,
    val updateFile: File? = null,
    val isUpdateDialogShowed: Boolean = false
)

class HostContext(
    val errorContext: ErrorContextImpl = ErrorContextImpl()
) : KoinComponent,
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl() {

    val repository: DeliveryRepository by inject()
    val updatesUseCase: AppUpdateUseCase by inject()
    val deviceUUIDProvider: DeviceUUIDProvider by inject()
    val locationProvider: LocationProvider by inject()
    val pauseRepository: PauseRepository by inject()
    val taskEventController: TaskEventController by inject()
    val serviceEventController: ServiceEventController by inject()
    val userRepository: CurrentUserStorage by inject()
    val settings: SettingsRepository by inject()

    var copyToClipboard: (String) -> Unit = {}
    var showUpdateDialog: (AppUpdate) -> Boolean = { false }
    var showErrorDialog: (id: Int) -> Unit = {}
    var installUpdate: (updateFile: File) -> Unit = {}
    var showPauseDialog: (availablePauseTypes: List<PauseType>) -> Unit = {}
    var showTaskUpdateRequired: (canSkip: Boolean) -> Unit = {}

    var featureCheckersContainer: FeatureCheckersContainer? = null

    var finishApp: () -> Unit = {}
}

typealias HostRender = ElmRender<HostState>
typealias HostMessage = ElmMessage<HostContext, HostState>
typealias HostEffect = ElmEffect<HostContext, HostState>
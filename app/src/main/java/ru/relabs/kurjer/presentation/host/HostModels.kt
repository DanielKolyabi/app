package ru.relabs.kurjer.presentation.host

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.providers.DeviceUUIDProvider
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.useCases.LoginUseCase
import ru.relabs.kurjer.presentation.base.fragment.AppBarSettings
import ru.relabs.kurjer.presentation.base.tea.*

/**
 * Created by Daniil Kurchanov on 20.11.2019.
 */
data class HostState(
    val settings: AppBarSettings = AppBarSettings(),
    val unreadAlertsCount: Int = 0,
    val loaders: Int = 0
)

class HostContext(
    val errorContext: ErrorContextImpl = ErrorContextImpl()
) : KoinComponent,
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl() {

    val repository: DeliveryRepository by inject()
    val deviceUUIDProvider: DeviceUUIDProvider by inject()

    var copyToClipboard: (String) -> Unit = {}
}

typealias HostRender = ElmRender<HostState>
typealias HostMessage = ElmMessage<HostContext, HostState>
typealias HostEffect = ElmEffect<HostContext, HostState>
package ru.relabs.kurjer.domain.useCases

import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.data.models.common.EitherE
import ru.relabs.kurjer.domain.mappers.UpdatesMapper
import ru.relabs.kurjer.domain.models.AppUpdatesInfo
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.utils.Right

class AppUpdateUseCase(
    val deliveryRepository: DeliveryRepository
) {
    private var requiredAppVersion: Int? = null
    val isAppUpdated: Boolean
        get() = requiredAppVersion?.let { it < BuildConfig.VERSION_CODE } ?: true

    suspend fun getAppUpdatesInfo(): EitherE<AppUpdatesInfo>{
        val result = deliveryRepository.getAppUpdatesInfo()
        if(result is Right){
            requiredAppVersion = result.value.required?.version
        }
        return result
    }
}
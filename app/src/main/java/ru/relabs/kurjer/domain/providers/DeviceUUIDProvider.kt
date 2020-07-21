package ru.relabs.kurjer.domain.providers

import ru.relabs.kurjer.domain.storage.AppPreferences
import java.util.*

class DeviceUUIDProvider(
    val appPreferences: AppPreferences
) {
    fun getOrGenerateDeviceUUID(): String =
        appPreferences.getDeviceUUID() ?: UUID.randomUUID().toString().also {
            appPreferences.saveDeviceUUID(it)
        }
}
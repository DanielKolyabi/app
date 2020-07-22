package ru.relabs.kurjer.domain.providers

import ru.relabs.kurjer.domain.models.DeviceId
import ru.relabs.kurjer.domain.storage.AppPreferences
import java.util.*

class DeviceUUIDProvider(
    private val appPreferences: AppPreferences
) {
    private var cachedDeviceId: DeviceId? = null

    fun getOrGenerateDeviceUUID(): DeviceId {
        val capturedCache = cachedDeviceId

        return if (capturedCache == null) {
            val generatedId = appPreferences.getDeviceUUID() ?: DeviceId(UUID.randomUUID().toString()).also {
                appPreferences.saveDeviceUUID(it)
            }
            cachedDeviceId = generatedId
            generatedId
        } else {
            capturedCache
        }
    }

}


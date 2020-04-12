package ru.relabs.kurjer.repository

import android.content.SharedPreferences
import kotlinx.coroutines.experimental.*
import ru.relabs.kurjer.network.DeliveryServerAPI
import ru.relabs.kurjer.utils.tryOrLogAsync

/**
 * Created by Daniil Kurchanov on 13.01.2020.
 */
class RadiusRepository(
        private val api: DeliveryServerAPI.IDeliveryServerAPI,
        private val sharedPreferences: SharedPreferences,
        private val tokenProvider: () -> String?
) {
    var isRadiusRequired: Boolean = true
    var radius: Int = 50
    private var updateJob: Job? = null

    init {
        sharedPreferences.apply {
            isRadiusRequired = getBoolean(RADIUS_REQUIRED_KEY, isRadiusRequired)
            radius = getInt(RADIUS_KEY, radius)
        }
    }

    fun resetData() {
        sharedPreferences.edit()
                .remove(RADIUS_REQUIRED_KEY)
                .remove(RADIUS_KEY)
                .apply()
        isRadiusRequired = false
        radius = 50
    }

    suspend fun startRemoteUpdating() {
        updateJob?.cancel()
        updateJob = launch(DefaultDispatcher) {
            while (isActive) {
                loadRadiusRemote()
                delay(30 * 1000)
            }
        }
    }

    suspend fun loadRadiusRemote() = withContext(DefaultDispatcher) {
        val token = tokenProvider() ?: return@withContext
        tryOrLogAsync {
            val response = api.getRadius(token).await()
            sharedPreferences.edit()
                    .putBoolean(RADIUS_REQUIRED_KEY, response.locked)
                    .putInt(RADIUS_KEY, response.radius)
                    .apply()

            isRadiusRequired = response.locked
            radius = response.radius
        }
    }

    companion object {
        const val RADIUS_REQUIRED_KEY = "radius_required"
        const val RADIUS_KEY = "radius"
    }
}
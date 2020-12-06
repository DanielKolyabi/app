package ru.relabs.kurjer.domain.repositories

import android.content.SharedPreferences
import kotlinx.coroutines.*
import ru.relabs.kurjer.domain.models.AllowedCloseRadius
import ru.relabs.kurjer.utils.Right

/**
 * Created by Daniil Kurchanov on 13.01.2020.
 */
class RadiusRepository(
    private val api: DeliveryRepository,
    private val sharedPreferences: SharedPreferences
) {
    val scope = CoroutineScope(Dispatchers.Main)
    var allowedCloseRadius: AllowedCloseRadius = loadSavedRadius()

    private var updateJob: Job? = null

    fun resetData() {
        allowedCloseRadius = AllowedCloseRadius.Required(DEFAULT_REQUIRED_RADIUS, false)
        sharedPreferences.edit()
            .remove(RADIUS_REQUIRED_KEY)
            .remove(RADIUS_KEY)
            .remove(PHOTO_REQUIRED_KEY)
            .apply()
    }

    suspend fun startRemoteUpdating() {
        updateJob?.cancel()
        updateJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                loadRadiusRemote()
                delay(60 * 1000)
            }
        }
    }

    suspend fun loadRadiusRemote() = withContext(Dispatchers.Default) {
        when (val r = api.getAllowedCloseRadius()) {
            is Right -> {
                allowedCloseRadius = r.value
                saveRadius(allowedCloseRadius)
            }
        }
    }

    private fun loadSavedRadius(): AllowedCloseRadius {
        val closeRequired = sharedPreferences.getBoolean(RADIUS_REQUIRED_KEY, true)
        val radius = sharedPreferences.getInt(RADIUS_KEY, DEFAULT_REQUIRED_RADIUS)
        val photoRequired = sharedPreferences.getBoolean(PHOTO_REQUIRED_KEY, true)
        return if (!closeRequired) {
            AllowedCloseRadius.NotRequired(radius, photoRequired)
        } else {
            AllowedCloseRadius.Required(radius, photoRequired)
        }
    }

    private fun saveRadius(allowedCloseRadius: AllowedCloseRadius) {
        val editor = sharedPreferences.edit()
        when (allowedCloseRadius) {
            is AllowedCloseRadius.NotRequired -> {
                editor.putBoolean(RADIUS_REQUIRED_KEY, false)
                editor.putBoolean(PHOTO_REQUIRED_KEY, allowedCloseRadius.photoAnyDistance)
                editor.putInt(RADIUS_KEY, allowedCloseRadius.distance)
            }
            is AllowedCloseRadius.Required -> {
                editor.putBoolean(RADIUS_REQUIRED_KEY, true)
                editor.putBoolean(PHOTO_REQUIRED_KEY, allowedCloseRadius.photoAnyDistance)
                editor.putInt(RADIUS_KEY, allowedCloseRadius.distance)
            }
        }
        editor.apply()
    }

    companion object {
        const val RADIUS_REQUIRED_KEY = "radius_required"
        const val PHOTO_REQUIRED_KEY = "photo_required"
        const val RADIUS_KEY = "radius"
        const val DEFAULT_REQUIRED_RADIUS = 50
    }
}
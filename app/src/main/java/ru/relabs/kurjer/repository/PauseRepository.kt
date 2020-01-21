package ru.relabs.kurjer.repository

import android.content.SharedPreferences
import kotlinx.coroutines.experimental.*
import retrofit2.HttpException
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.ReportService
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI
import ru.relabs.kurjer.network.models.ErrorUtils
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.entities.SendQueryItemEntity
import ru.relabs.kurjer.utils.application
import ru.relabs.kurjer.utils.currentTimestamp
import ru.relabs.kurjer.utils.tryOrLogAsync
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Daniil Kurchanov on 06.01.2020.
 */

enum class PauseType {
    Lunch, Load
}

class PauseRepository(
        private val api: DeliveryServerAPI.IDeliveryServerAPI,
        private val sharedPreferences: SharedPreferences,
        private val db: AppDatabase,
        private val tokenProvider: () -> String?
) {
    private var lunchTime: Int = 20 * 60
    private var loadTime: Int = 20 * 60
    private var currentPauseType: PauseType? = null
    private var pauseEndJob: Job? = null

    var isPaused: Boolean = false

    init {
        lunchTime = sharedPreferences.getInt(LUNCH_KEY, lunchTime)
        loadTime = sharedPreferences.getInt(LOAD_KEY, loadTime)

        fun startPauseEndJob(delay: Long) {
            pauseEndJob = launch(CommonPool) {
                delay(delay * 1000)
                isPaused = false
            }
        }

        val currentTime = currentTimestamp()
        if (getPauseStartTime(PauseType.Load) + getPauseLength(PauseType.Load) > currentTime) {
            val delta = currentTime - getPauseStartTime(PauseType.Load) + getPauseLength(PauseType.Load)
            startPauseEndJob(delta)
            isPaused = true
        } else if (getPauseStartTime(PauseType.Lunch) + getPauseLength(PauseType.Lunch) > currentTime) {
            val delta = currentTime - getPauseStartTime(PauseType.Lunch) + getPauseLength(PauseType.Lunch)
            startPauseEndJob(delta)
            isPaused = true
        }
    }

    suspend fun loadPauseTime() = withContext(DefaultDispatcher) {
        tryOrLogAsync {
            val response = api.getPauseTimes().await()
            sharedPreferences.edit()
                    .putInt(LUNCH_KEY, response.lunch.toInt())
                    .putInt(LOAD_KEY, response.loading.toInt())
                    .apply()
        }
    }

    fun getPauseStartTime(type: PauseType): Long {
        return sharedPreferences.getLong(when (type) {
            PauseType.Lunch -> LUNCH_LAST_TIME_KEY
            PauseType.Load -> LOAD_LAST_TIME_KEY
        }, 0)
    }

    fun getPauseLength(type: PauseType): Int {
        return when (type) {
            PauseType.Lunch -> lunchTime
            PauseType.Load -> loadTime
        }
    }

    fun putPauseStartTime(type: PauseType, time: Long, startIfNeed: Boolean = false) {
        sharedPreferences.edit()
                .putLong(when (type) {
                    PauseType.Lunch -> LUNCH_LAST_TIME_KEY
                    PauseType.Load -> LOAD_LAST_TIME_KEY
                }, time)
                .apply()

        if (startIfNeed) {
            val currentTime = currentTimestamp()

            val shouldStartPause = when (type) {
                PauseType.Lunch -> currentTime - time < lunchTime
                PauseType.Load -> currentTime - time < loadTime
            }

            if (shouldStartPause) {
                startPause(type, time)
            }
        }
    }

    suspend fun isPauseAvailableRemote(type: PauseType): Boolean {
        val pauseType = when (type) {
            PauseType.Lunch -> 0
            PauseType.Load -> 1
        }

        return try {
            api.isPauseAllowed((application().user as UserModel.Authorized).token, pauseType).await().status
        } catch (e: HttpException) {
            val remotePauseTime = ErrorUtils.getError(e).data["last_pause"] as? Long
            remotePauseTime?.let {
                putPauseStartTime(type, remotePauseTime, true)
            }
            true
        } catch (e: Exception) {
            true
        }
    }

    fun isAnyPauseAvailable(): Boolean {
        return !isPaused && (isPauseAvailable(PauseType.Load) || isPauseAvailable(PauseType.Lunch))
    }

    fun isPauseAvailable(type: PauseType): Boolean {
        val lastPauseTimeStamp = sharedPreferences.getLong(when (type) {
            PauseType.Lunch -> LUNCH_LAST_TIME_KEY
            PauseType.Load -> LOAD_LAST_TIME_KEY
        }, 0)

        val lastPauseTime = Calendar.getInstance().apply {
            timeInMillis = lastPauseTimeStamp * 1000
        }

        val time = Calendar.getInstance(TimeZone.getTimeZone("GMT+3:00"))

        when (type) {
            PauseType.Lunch -> if (lastPauseTime.get(Calendar.YEAR) == time.get(Calendar.YEAR)
                    && lastPauseTime.get(Calendar.MONTH) == time.get(Calendar.MONTH)
                    && lastPauseTime.get(Calendar.DATE) == time.get(Calendar.DATE)) {
                return false
            }
            PauseType.Load -> if (currentTimestamp() - lastPauseTimeStamp < 2 * 60 * 60 + loadTime) {
                return false
            }
        }

        return true
    }

    fun startPause(type: PauseType, time: Long? = null) {
        if (isPaused) {
            return
        }

        isPaused = true

        currentPauseType = type

        val currentTime = currentTimestamp()

        val pauseType = when (type) {
            PauseType.Lunch -> 0
            PauseType.Load -> 1
        }
        val pauseTime = time ?: currentTime
        val pauseEndTime = pauseTime + when (type) {
            PauseType.Lunch -> lunchTime
            PauseType.Load -> loadTime
        }

        pauseEndJob?.cancel()
        pauseEndJob = launch(CommonPool) {
            delay(pauseEndTime - currentTime, TimeUnit.SECONDS)
            isPaused = false
            currentPauseType = null
        }

        ReportService.instance?.pauseTimer(type, pauseEndTime)
        putPauseStartTime(type, pauseTime)
        db.sendQueryDao().insert(
                SendQueryItemEntity(
                        0,
                        BuildConfig.API_URL + "/api/v1/pause/start?token=" + (application().user as UserModel.Authorized).token,
                        "type=$pauseType&time=${pauseTime}"
                )
        )
    }

    fun stopPause(type: PauseType? = null, time: Long? = null) {
        if (!isPaused) {
            return
        }

        val type = type ?: getActivePauseType() ?: return
        pauseEndJob?.cancel()

        val pauseType = when (type) {
            PauseType.Lunch -> 0
            PauseType.Load -> 1
        }

        isPaused = false
        currentPauseType = null

        val pauseTime = time ?: currentTimestamp()
        db.sendQueryDao().insert(
                SendQueryItemEntity(
                        0,
                        BuildConfig.API_URL + "/api/v1/pause/stop?token=" + (application().user as UserModel.Authorized).token,
                        "type=$pauseType&time=${pauseTime}"
                )
        )
    }

    private fun getActivePauseType(): PauseType? {
        currentPauseType?.let {
            return currentPauseType
        }

        val currentTime = currentTimestamp()
        listOf(PauseType.Lunch, PauseType.Load).forEach {
            if (getPauseStartTime(it) + getPauseLength(it) > currentTime) {
                return it
            }
        }
        return null
    }

    suspend fun loadLastPausesRemote() = withContext(DefaultDispatcher){
        val token = tokenProvider() ?: return@withContext
        tryOrLogAsync {
            val response = api.getLastPauseTimes(token).await()
            putPauseStartTime(PauseType.Load, response.loading.toLong(), true)
            putPauseStartTime(PauseType.Lunch, response.lunch.toLong(), true)
        }
    }

    fun resetData() {
        sharedPreferences.edit()
                .remove(LUNCH_LAST_TIME_KEY)
                .remove(LOAD_LAST_TIME_KEY)
                .apply()
        currentPauseType = null
        pauseEndJob?.cancel()
        isPaused = false
    }

    companion object {
        const val LUNCH_KEY = "lunch_pause"
        const val LOAD_KEY = "load_pause"
        const val TASK_CLOSE_KEY = "task_close_pause"

        const val LUNCH_LAST_TIME_KEY = "lunch_last_time"
        const val LOAD_LAST_TIME_KEY = "load_last_time"
    }
}
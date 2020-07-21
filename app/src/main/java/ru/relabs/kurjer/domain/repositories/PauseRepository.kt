package ru.relabs.kurjer.domain.repositories

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.*
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.ReportService
import ru.relabs.kurjer.data.models.common.DomainException
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.entities.SendQueryItemEntity
import ru.relabs.kurjer.utils.*
import java.util.*

/**
 * Created by Daniil Kurchanov on 06.01.2020.
 */

enum class PauseType {
    Lunch, Load
}

class PauseRepository(
    private val api: DeliveryRepository,
    private val sharedPreferences: SharedPreferences,
    private val db: AppDatabase
) {
    private var lunchDuration: Int = 20 * 60
    private var loadDuration: Int = 20 * 60
    private var currentPauseType: PauseType? = null
    private var pauseEndJob: Job? = null

    var isPaused: Boolean = false

    init {
        lunchDuration = sharedPreferences.getInt(LUNCH_KEY, lunchDuration)
        loadDuration = sharedPreferences.getInt(LOAD_KEY, loadDuration)
    }

    suspend fun loadPauseDurations() = withContext(Dispatchers.Default) {
        when (val r = api.getPauseDurations()) {
            is Right -> {
                sharedPreferences.edit()
                    .putInt(LUNCH_KEY, r.value.lunch.toInt())
                    .putInt(LOAD_KEY, r.value.loading.toInt())
                    .apply()
                lunchDuration = r.value.lunch.toInt()
                loadDuration = r.value.loading.toInt()
            }
            is Left -> TODO("Handle error")
        }
    }

    fun getPauseStartTime(type: PauseType): Long {
        return sharedPreferences.getLong(
            when (type) {
                PauseType.Lunch -> LUNCH_LAST_START_TIME_KEY
                PauseType.Load -> LOAD_LAST_START_TIME_KEY
            }, 0
        )
    }

    fun getPauseEndTime(type: PauseType): Long {
        return sharedPreferences.getLong(
            when (type) {
                PauseType.Lunch -> LUNCH_LAST_END_TIME_KEY
                PauseType.Load -> LOAD_LAST_END_TIME_KEY
            }, 0
        )
    }

    fun getPauseLength(type: PauseType): Int {
        return when (type) {
            PauseType.Lunch -> lunchDuration
            PauseType.Load -> loadDuration
        }
    }

    fun putPauseStartTime(type: PauseType, time: Long) {
        sharedPreferences.edit()
            .putLong(
                when (type) {
                    PauseType.Lunch -> LUNCH_LAST_START_TIME_KEY
                    PauseType.Load -> LOAD_LAST_START_TIME_KEY
                }, time
            )
            .apply()
    }

    fun putPauseEndTime(type: PauseType, time: Long) {
        sharedPreferences.edit()
            .putLong(
                when (type) {
                    PauseType.Lunch -> LUNCH_LAST_END_TIME_KEY
                    PauseType.Load -> LOAD_LAST_END_TIME_KEY
                }, time
            )
            .apply()
    }

    suspend fun isPauseAvailableRemote(type: PauseType): Boolean =
        when (val r = api.isPauseAllowed(type)) {
            is Right -> r.value
            is Left -> when (val e = r.value) {
                is DomainException.ApiException -> when (val remotePauseTime = e.error.details["last_pause"]) {
                    is Long -> {
                        putPauseStartTime(type, remotePauseTime)
                        true
                    }
                    else -> true
                }
                else -> true
            }
        }


    fun isPauseAvailable(type: PauseType): Boolean {
        val lastPauseTimeStamp = sharedPreferences.getLong(
            when (type) {
                PauseType.Lunch -> LUNCH_LAST_START_TIME_KEY
                PauseType.Load -> LOAD_LAST_START_TIME_KEY
            }, 0
        )

        val lastPauseTime = Calendar.getInstance().apply {
            timeInMillis = lastPauseTimeStamp * 1000
        }

        val time = Calendar.getInstance(TimeZone.getTimeZone("GMT+3:00"))

        when (type) {
            PauseType.Lunch -> if (lastPauseTime.get(Calendar.YEAR) == time.get(Calendar.YEAR)
                && lastPauseTime.get(Calendar.MONTH) == time.get(Calendar.MONTH)
                && lastPauseTime.get(Calendar.DATE) == time.get(Calendar.DATE)
            ) {
                return false
            }
            PauseType.Load -> if (currentTimestamp() - lastPauseTimeStamp < 2 * 60 * 60 + loadDuration) {
                return false
            }
        }

        return true
    }

    fun startPause(type: PauseType, time: Long? = null, withNotify: Boolean = true) {
        CustomLog.writeToFile("Start pause: $type, time: $time, isPaused: $isPaused, currentPauseType: $currentPauseType, withNotify: $withNotify")
        Log.d(
            "PauseRepository",
            "Start pause: $type, time: $time, isPaused: $isPaused, currentPauseType: $currentPauseType, withNotify: $withNotify"
        )
        if (isPaused) {
            return
        }

        val currentTime = currentTimestamp()

        val pauseType = when (type) {
            PauseType.Lunch -> 0
            PauseType.Load -> 1
        }
        val pauseTime = time ?: currentTime
        val pauseEndTime = pauseTime + when (type) {
            PauseType.Lunch -> lunchDuration
            PauseType.Load -> loadDuration
        }

        pauseEndJob?.cancel()
        pauseEndJob = GlobalScope.launch(Dispatchers.Default) {
            delay((pauseEndTime - currentTime + 10) * 1000)
            updatePauseState()
        }

        isPaused = true
        currentPauseType = type

        ReportService.instance?.pauseTimer(pauseTime, pauseEndTime)
        putPauseStartTime(type, pauseTime)
        if (withNotify) {
            db.sendQueryDao().insert(
                SendQueryItemEntity(
                    0,
                    BuildConfig.API_URL + "/api/v1/pause/start?token=" + (application().user as UserModel.Authorized).token,
                    "type=$pauseType&time=${pauseTime}"
                )
            )
        }
    }

    fun stopPause(type: PauseType? = null, time: Long? = null, withNotify: Boolean, withUpdate: Boolean) {
        CustomLog.writeToFile("Stop pause: $type, time: $time, isPaused: $isPaused, currentPauseType: $currentPauseType, withNotify: $withNotify")
        Log.d(
            "PauseRepository",
            "Stop pause: $type, time: $time, isPaused: $isPaused, currentPauseType: $currentPauseType, withNotify: $withNotify"
        )
        if (!isPaused) {
            return
        }

        val type = type ?: getActivePauseType() ?: return
        pauseEndJob?.cancel()

        val pauseType = when (type) {
            PauseType.Lunch -> 0
            PauseType.Load -> 1
        }

        val pauseTime = time ?: currentTimestamp()
        if (withNotify) {
            db.sendQueryDao().insert(
                SendQueryItemEntity(
                    0,
                    BuildConfig.API_URL + "/api/v1/pause/stop?token=" + (application().user as UserModel.Authorized).token,
                    "type=$pauseType&time=${pauseTime}"
                )
            )
        }
        isPaused = false
        currentPauseType = null
        putPauseEndTime(type, pauseTime)
        if (withUpdate) {
            updatePauseState()
        }
    }

    fun updatePauseState() {
        updatePauseState(PauseType.Lunch)
        updatePauseState(PauseType.Load)
    }

    fun updatePauseState(type: PauseType) {
        val currentTime = currentTimestamp()
        val lastPauseStartTime = getPauseStartTime(type)
        val lastPauseEndTime = getPauseEndTime(type)
        val duration = when (type) {
            PauseType.Lunch -> lunchDuration
            PauseType.Load -> loadDuration
        }
        Log.d("PauseRepository", "type: $type, lastStart: $lastPauseStartTime, lastEnd: $lastPauseEndTime, dur: $duration")
        //Pause already ended from BE
        if (lastPauseEndTime > lastPauseStartTime) {
            if (isPaused && currentPauseType == type) {
                stopPause(type, withNotify = false, withUpdate = false)
            }
            return
        }

        //Pause ended from device time
        if (currentTime - lastPauseStartTime > duration) {
            if (isPaused && currentPauseType == type) {
                stopPause(type, withNotify = true, withUpdate = false)
            }
            return
        }

        if (!isPaused) {
            startPause(type, lastPauseStartTime, withNotify = false)
        }
    }

    private fun getActivePauseType(): PauseType? {
        currentPauseType?.let {
            return currentPauseType
        }

        val currentTime = currentTimestamp()
        listOf(
            PauseType.Lunch,
            PauseType.Load
        ).forEach {
            if (getPauseStartTime(it) + getPauseLength(it) > currentTime) {
                return it
            }
        }
        return null
    }

    suspend fun loadLastPausesRemote() = withContext(Dispatchers.Default) {
        when (val r = api.getLastPauseTimes()) {
            is Right -> {
                putPauseStartTime(PauseType.Load, r.value.start.loading)
                putPauseStartTime(PauseType.Lunch, r.value.start.lunch)
                putPauseEndTime(PauseType.Load, r.value.end.loading)
                putPauseEndTime(PauseType.Lunch, r.value.end.lunch)
                updatePauseState()
            }
            is Left -> TODO("Handle error")
        }
    }

    fun resetData() {
        sharedPreferences.edit()
            .remove(LUNCH_LAST_START_TIME_KEY)
            .remove(LOAD_LAST_START_TIME_KEY)
            .remove(LUNCH_LAST_END_TIME_KEY)
            .remove(LOAD_LAST_END_TIME_KEY)
            .apply()
        currentPauseType = null
        pauseEndJob?.cancel()
        isPaused = false
    }

    companion object {
        const val LUNCH_KEY = "lunch_pause"
        const val LOAD_KEY = "load_pause"

        const val LUNCH_LAST_START_TIME_KEY = "lunch_last_start_time"
        const val LOAD_LAST_START_TIME_KEY = "load_last_start_time"
        const val LUNCH_LAST_END_TIME_KEY = "lunch_last_end_time"
        const val LOAD_LAST_END_TIME_KEY = "load_last_end_time"
    }
}
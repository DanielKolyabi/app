package ru.relabs.kurjer.domain.repositories

import android.content.SharedPreferences
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.models.common.DomainException
import ru.relabs.kurjer.domain.storage.CurrentUserStorage
import ru.relabs.kurjer.services.ReportService
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.currentTimestamp
import ru.relabs.kurjer.utils.debug
import java.util.*

/**
 * Created by Daniil Kurchanov on 06.01.2020.
 */

enum class PauseType {
    Lunch, Load
}

fun PauseType.toInt() = when (this) {
    PauseType.Lunch -> 0
    PauseType.Load -> 1
}

class PauseRepository(
    private val api: DeliveryRepository,
    private val sharedPreferences: SharedPreferences,
    private val db: DatabaseRepository,
    private val userStorage: CurrentUserStorage
) {

    private var lunchDuration: Int = 20 * 60
    private var loadDuration: Int = 20 * 60

    val isPaused
        get() = getActivePauseType() != null

    init {
        lunchDuration = sharedPreferences.getInt(LUNCH_KEY, lunchDuration)
        loadDuration = sharedPreferences.getInt(LOAD_KEY, loadDuration)

        val activePauseType = getActivePauseType()

        if (activePauseType != null) {
            val pauseEndTime = getPauseStartTime(activePauseType) + getPauseLength(activePauseType)
            ReportService.instance?.pauseTaskClosingTimer(getPauseStartTime(activePauseType), pauseEndTime)
        }
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
            else -> Unit
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

    suspend fun startPause(type: PauseType, time: Long? = null, withNotify: Boolean = true) {
        if (isPaused) {
            FirebaseCrashlytics.getInstance()
                .log("Pause started while paused ${userStorage.getCurrentUserLogin()}, withNotify: $withNotify")
        }

        val currentTime = currentTimestamp()

        val pauseTime = time ?: currentTime
        val pauseEndTime = pauseTime + when (type) {
            PauseType.Lunch -> lunchDuration
            PauseType.Load -> loadDuration
        }

        ReportService.instance?.pauseTaskClosingTimer(pauseTime, pauseEndTime)
        putPauseStartTime(type, pauseTime)
        if (withNotify) {
            db.putSendQuery(SendQueryData.PauseStart(type, pauseTime))
        }
    }

    suspend fun stopPause(type: PauseType? = null, withNotify: Boolean) {
        if (!isPaused) {
            FirebaseCrashlytics.getInstance()
                .log("Pause stopped while not paused ${userStorage.getCurrentUserLogin()}, withNotify: $withNotify")
        }

        val type = type ?: getActivePauseType() ?: return

        val pauseTime = currentTimestamp()
        putPauseEndTime(type, pauseTime)
        ReportService.instance?.startTaskClosingTimer(true)
        if (withNotify) {
            val r = db.putSendQuery(SendQueryData.PauseStop(type, pauseTime))
            debug("$r")
        }
    }

    private fun getActivePauseType(): PauseType? {
        val currentTime = currentTimestamp()
        listOf(
            PauseType.Lunch,
            PauseType.Load
        ).forEach {
            val start = getPauseStartTime(it)
            val end = getPauseEndTime(it)
            val duration = getPauseLength(it)
            if (start + duration > currentTime && end < getPauseStartTime(it)) {
                return it
            }
        }
        return null
    }

    suspend fun loadLastPausesRemote() = withContext(Dispatchers.Default) {
        when (val r = api.getLastPauseTimes()) {
            is Right -> {
                putPauseStartTime(PauseType.Load, r.value.loading.start)
                putPauseStartTime(PauseType.Lunch, r.value.lunch.start)
                putPauseEndTime(PauseType.Load, r.value.loading.end)
                putPauseEndTime(PauseType.Lunch, r.value.lunch.end)
            }
            is Left -> Unit
        }
    }

    fun resetData() {
        sharedPreferences.edit()
            .remove(LUNCH_LAST_START_TIME_KEY)
            .remove(LOAD_LAST_START_TIME_KEY)
            .remove(LUNCH_LAST_END_TIME_KEY)
            .remove(LOAD_LAST_END_TIME_KEY)
            .apply()
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
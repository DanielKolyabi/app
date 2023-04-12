package ru.relabs.kurjer.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.AlertNotificationActivity
import ru.relabs.kurjer.DeliveryApp
import ru.relabs.kurjer.R
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntity
import ru.relabs.kurjer.data.database.entities.SendQueryItemEntity
import ru.relabs.kurjer.data.database.entities.storage.StorageReportRequestEntity
import ru.relabs.kurjer.domain.controllers.ServiceEvent
import ru.relabs.kurjer.domain.controllers.ServiceEventController
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.StorageRepository
import ru.relabs.kurjer.utils.*


const val CHANNEL_ID = "notification_channel"
const val CLOSE_SERVICE_TIMEOUT = 80 * 60 * 1000
const val TIMELIMIT_NOTIFICATION_TIMEOUT = 30 * 60 * 1000
const val TASK_CHECK_DELAY = 10 * 60 * 1000

class ReportService : Service(), KoinComponent {
    private val repository: DeliveryRepository by inject()
    private val databaseRepository: DatabaseRepository by inject()
    private val taskEventController: TaskEventController by inject()
    private val serviceEventController: ServiceEventController by inject()
    private val storageRepository: StorageRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Default)

    private var timeUntilRun: Int = 0
    private var pauseDisableJob: Job? = null
    private var looperJob: Job? = null
    private var currentIconBitmap: Bitmap? = null
    private var lastState: ServiceState = ServiceState.IDLE
    private var lastActivityResumeTime = 0L
    private var lastActivityRunningState = false
    private var timelimitNotificationStartTime: Long? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    enum class ServiceState {
        TRANSFER, IDLE, UNAVAILABLE
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(KEY_START_CLOSING_TIMER, false) == true) {
            startTaskClosingTimer()
            return START_STICKY
        }
        if (intent?.getBooleanExtra(KEY_STOP_CLOSING_TIMER, false) == true) {
            stopTaskClosingTimer()
            return START_STICKY
        }


        instance = this

        startForeground(1, notification("Сервис отправки данных.", ServiceState.IDLE))
        isRunning = true

        var lastTasksChecking = System.currentTimeMillis()

        looperJob?.cancel()
        looperJob = scope.launch {
            while (isActive) {
                var isTaskSended = false
                if (NetworkHelper.isNetworkAvailable(applicationContext)) {
                    val sendQuery = databaseRepository.getNextSendQuery()
                    val reportQuery = databaseRepository.getNextReportQuery()
                    val storageReportQuery = storageRepository.getNextQuery()
                    when {
                        sendQuery != null -> {
                            when (val r = sendSendQuery(sendQuery)) {
                                is Left -> r.value.log()
                                is Right -> {
                                    isTaskSended = true
                                    databaseRepository.removeSendQuery(sendQuery)
                                }
                            }
                        }
                        storageReportQuery != null -> {
                            when (val r = sendStorageReportQuery(storageReportQuery)) {
                                is Left -> r.value.log()
                                is Right -> {
                                    isTaskSended = true
                                    storageRepository.removeStorageReportQuery(storageReportQuery)
                                    if (!storageRepository.isReportHasQuery(storageReportQuery.storageReportId)) {
                                        storageRepository.deletePhotosReportById(storageReportQuery.storageReportId)
                                        storageRepository.deleteReport(storageReportQuery.storageReportId)
                                    }
                                }
                            }
                        }
                        reportQuery != null -> {
                            when (val r = sendReportQuery(reportQuery)) {
                                is Left -> r.value.log()
                                is Right -> {
                                    isTaskSended = true
                                    databaseRepository.removeReport(reportQuery)
                                }
                            }
                        }
                        System.currentTimeMillis() - lastTasksChecking > TASK_CHECK_DELAY -> {
                            when (val tasks = repository.getTasks()) {
                                is Right -> if (databaseRepository.isMergeNeeded(tasks.value)) {
                                    CustomLog.writeToFile("UPDATE: merge is needed")
                                    taskEventController.send(TaskEvent.TasksUpdateRequired(false))
                                }
                                is Left -> Unit
                            }
                            lastTasksChecking = System.currentTimeMillis()
                        }
                    }
                }

                updateNotificationText()

                checkTimelimitJob()
                updateActivityState()
                delay(if (!isTaskSended) 5000 else 1000)
            }
        }

        return START_STICKY
    }

    private fun updateActivityState() {
        if (!lastActivityRunningState && !isAppPaused) {
            lastActivityResumeTime = System.currentTimeMillis()
        }

        if (isAppPaused && (System.currentTimeMillis() - lastActivityResumeTime) > CLOSE_SERVICE_TIMEOUT) {
            serviceEventController.send(ServiceEvent.Stop)
            stopSelf()
            stopForeground(true)
        }
        lastActivityRunningState = !isAppPaused
    }


    private fun checkTimelimitJob() {
        val startTime = timelimitNotificationStartTime
        startTime ?: return

        if (System.currentTimeMillis() > startTime + TIMELIMIT_NOTIFICATION_TIMEOUT) {
            scope.launch {
                timelimitNotificationStartTime = null
                if (!databaseRepository.isOpenedTasksExists()) {
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    val intent = Intent(applicationContext, AlertNotificationActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.addFlags(Intent.FLAG_FROM_BACKGROUND)
                    startActivity(intent, Bundle())
                }
            }
        }
    }

    fun startTaskClosingTimer(fromPause: Boolean = false) {
        pauseDisableJob?.cancel()
        timelimitNotificationStartTime =
            System.currentTimeMillis() - ((TIMELIMIT_NOTIFICATION_TIMEOUT - timeUntilRun).takeIf { timeUntilRun != 0 && fromPause }
                ?: 0)
        timeUntilRun = 0
    }

    @SuppressLint("MissingPermission")
    private suspend fun updateNotificationText() {
        val isNetworkAvailable = NetworkHelper.isNetworkAvailable(applicationContext)
        val count = databaseRepository.getQueryItemsCount()
        val state = if (!isNetworkAvailable) {
            ServiceState.UNAVAILABLE
        } else if (count > 0) {
            ServiceState.TRANSFER
        } else {
            ServiceState.IDLE
        }

        val text = resources.getString(R.string.report_service_query_size, count) +
                (resources.getString(R.string.report_service_network_disabled)
                    .takeIf { !NetworkHelper.isNetworkAvailable(this) } ?: "")

        NotificationManagerCompat
            .from(this)
            .notify(1, notification(text, state))
    }

    private suspend fun sendStorageReportQuery(item: StorageReportRequestEntity): Either<java.lang.Exception, Unit> =
        repository.sendStorageReport(item)

    private suspend fun sendReportQuery(item: ReportQueryItemEntity): Either<java.lang.Exception, Unit> =
        repository.sendReport(item)

    private suspend fun sendSendQuery(item: SendQueryItemEntity): Either<java.lang.Exception, Unit> =
        repository.sendQuery(item)

    private fun stopTaskClosingTimer() {
        timelimitNotificationStartTime = null
    }

    fun pauseTaskClosingTimer(startTime: Long, endTime: Long) {
        val timeLimitStart = timelimitNotificationStartTime ?: return
        timelimitNotificationStartTime = null
        timeUntilRun =
            (timeLimitStart + TIMELIMIT_NOTIFICATION_TIMEOUT - System.currentTimeMillis()).toInt()
        pauseDisableJob?.cancel()
        pauseDisableJob = scope.launch {
            delay((endTime - currentTimestamp()) * 1000)
            if (!isActive) return@launch
            startTaskClosingTimer(true)
        }
    }

    override fun onDestroy() {
        looperJob?.cancel()
        stopForeground(true)
        isRunning = false
        super.onDestroy()
    }

    private fun notification(
        body: String,
        status: ServiceState,
        update: Boolean = false
    ): Notification {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID, getString(
                    R.string.app_name
                ), NotificationManager.IMPORTANCE_HIGH
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                notificationChannel
            )
        }

        val ic = when (status) {
            ServiceState.TRANSFER -> R.drawable.ic_service_ok
            ServiceState.IDLE -> R.drawable.ic_service_idle
            ServiceState.UNAVAILABLE -> R.drawable.ic_service_error
        }
        if (lastState != status) {
            currentIconBitmap?.recycle()
            currentIconBitmap = BitmapFactory.decodeResource(application.resources, ic)
            lastState = status
        }

        val millisToClose =
            CLOSE_SERVICE_TIMEOUT - (System.currentTimeMillis() - lastActivityResumeTime)
        val closeNotifyText =
            if (status == ServiceState.IDLE && !lastActivityRunningState && millisToClose > 0) {
                val secondsToClose = millisToClose / 1000
                val timeWithUnit = if (secondsToClose < 60) {
                    secondsToClose to "сек"
                } else {
                    (secondsToClose / 60).toInt() to "мин"
                }
                " Закрытие через ${timeWithUnit.first} ${timeWithUnit.second}."
            } else {
                ""
            }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(body + closeNotifyText)
            .setSmallIcon(ic)
            .setLargeIcon(currentIconBitmap)
            .setWhen(System.currentTimeMillis())
            .setChannelId(CHANNEL_ID)
            .setOnlyAlertOnce(true)

        return notification.build()
    }

    companion object {
        var isRunning: Boolean = false
        var instance: ReportService? = null
        var isAppPaused: Boolean = false
        private val appCtx: Context = DeliveryApp.appContext

        const val KEY_START_CLOSING_TIMER = "start_closing_timer"
        const val KEY_STOP_CLOSING_TIMER = "stop_closing_timer"

        suspend fun restartTaskClosingTimer() = withContext(Dispatchers.Main) {
            restartTaskClosingTimerSync()
        }

        suspend fun stopTaskClosingTimer() = withContext(Dispatchers.Main) {
            stopTaskClosingTimerSync()
        }

        fun restartTaskClosingTimerSync() =
            appCtx.startService(Intent(appCtx, ReportService::class.java).apply {
                putExtra(KEY_START_CLOSING_TIMER, true)
            })

        fun stopTaskClosingTimerSync() =
            appCtx.startService(Intent(appCtx, ReportService::class.java).apply {
                putExtra(KEY_STOP_CLOSING_TIMER, true)
            })
    }
}

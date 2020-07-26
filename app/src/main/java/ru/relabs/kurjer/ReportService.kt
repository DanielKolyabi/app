package ru.relabs.kurjer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntity
import ru.relabs.kurjer.data.database.entities.SendQueryItemEntity
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.log
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


const val CHANNEL_ID = "notification_channel"
const val CLOSE_SERVICE_TIMEOUT = 80 * 60 * 1000
const val TIMELIMIT_NOTIFICATION_TIMEOUT = 30 * 60 * 1000
const val TASK_CHECK_DELAY = 10 * 60 * 1000

class ReportService : Service(), KoinComponent {
    private val repository: DeliveryRepository by inject()
    private val database: AppDatabase by inject()

    private var timeUntilRun: Int = 0
    private var pauseDisableJob: Job? = null
    private var thread: Job? = null
    private var currentIconBitmap: Bitmap? = null
    private var lastState: ServiceState = ServiceState.IDLE
    private var lastActivityResumeTime = 0L
    private var lastActivityRunningState = false
    private var lastGPSPending = System.currentTimeMillis()
    private var timelimitNotificationStartTime: Long? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    enum class ServiceState {
        TRANSFER, IDLE, UNAVAILABLE
    }

    private fun notification(body: String, status: ServiceState, update: Boolean = false): Notification {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(notificationChannel)
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

        val millisToClose = CLOSE_SERVICE_TIMEOUT - (System.currentTimeMillis() - lastActivityResumeTime)
        val closeNotifyText = if (status == ServiceState.IDLE && !lastActivityRunningState && millisToClose > 0) {
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra("start_closing_timer", false) == true) {
            startTaskClosingTimer()
            return START_STICKY
        }

        instance = this

        startForeground(1, notification("Сервис отправки данных.", ServiceState.IDLE))
        isRunning = true

        var lastTasksChecking = System.currentTimeMillis()
        if (intent?.getBooleanExtra("force_check_updates", false) == true) {
            lastTasksChecking -= TASK_CHECK_DELAY
        }
        var lastNetworkEnabledChecking = System.currentTimeMillis()

        thread?.cancel()
        thread = GlobalScope.launch {
            while (true) {
                var isTaskSended = false

                if (NetworkHelper.isNetworkAvailable(applicationContext)) {
                    val sendQuery = getSendQuery(database)
                    val reportQuery = getReportQuery(database)

                    if (reportQuery != null) {
                        try {
                            sendReportQuery(reportQuery)
                            isTaskSended = true
                            PersistenceHelper.removeReport(database, reportQuery)
                        } catch (e: Exception) {
                            e.log()
                        }
                    } else if (sendQuery != null) {
                        try {
                            sendSendQuery(sendQuery)
                            isTaskSended = true
                            database.sendQueryDao().delete(sendQuery)
                        } catch (e: Exception) {
                            e.log()
                        }
                    } else if (System.currentTimeMillis() - lastTasksChecking > TASK_CHECK_DELAY) {
                        when (val tasks = repository.getTasks()) {
                            is Right -> if (PersistenceHelper.isMergeNeeded(database, tasks.value)) {
                                val int = Intent().apply {
                                    putExtra("tasks_changed", true)
                                    action = "NOW"
                                }
                                sendBroadcast(int)
                            }
                        }
                        lastTasksChecking = System.currentTimeMillis()
                    }
                }

                if (System.currentTimeMillis() - lastNetworkEnabledChecking > 10 * 60 * 1000) {
                    lastNetworkEnabledChecking = System.currentTimeMillis()
                    if (!NetworkHelper.isNetworkEnabled(applicationContext)) {
                        val int = Intent().apply {
                            putExtra("network_disabled", true)
                            action = "NOW"
                        }
                        sendBroadcast(int)
                    }
                }

                updateNotificationText(database)

                checkTimelimitJob()
                pendingGPS()
                updateActivityState()
                delay(if (!isTaskSended) 5000 else 1000)
            }
        }

        return START_STICKY
    }

    private fun pendingGPS() {
        if (System.currentTimeMillis() - lastGPSPending > 1 * 60 * 1000) {
            (DeliveryApp.appContext as DeliveryApp).requestLocation()
            lastGPSPending = System.currentTimeMillis()
        }
    }

    private fun updateActivityState() {
        if (!lastActivityRunningState && MainActivity.isRunning) {
            lastActivityResumeTime = System.currentTimeMillis()
        }

        if (!MainActivity.isRunning && (System.currentTimeMillis() - lastActivityResumeTime) > CLOSE_SERVICE_TIMEOUT) {
            val int = Intent().apply {
                putExtra("force_finish", true)
                action = "NOW"
            }
            sendBroadcast(int)

            stopSelf()
            stopForeground(true)
        }
        lastActivityRunningState = MainActivity.isRunning
    }


    private fun checkTimelimitJob() {
        val startTime = timelimitNotificationStartTime
        startTime ?: return

        if (System.currentTimeMillis() > startTime + TIMELIMIT_NOTIFICATION_TIMEOUT) {
            GlobalScope.launch {
                timelimitNotificationStartTime = null
                if (database.taskDao().allOpened.isEmpty()) {
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

    private fun updateNotificationText(db: AppDatabase) {
        val isNetworkAvailable = NetworkHelper.isNetworkAvailable(applicationContext)
        val count = db.sendQueryDao().all.size + db.reportQueryDao().all.size
        val state = if (!isNetworkAvailable) {
            ServiceState.UNAVAILABLE
        } else if (count > 0) {
            ServiceState.TRANSFER
        } else {
            ServiceState.IDLE
        }

        val text = "Сервис. В очереди: $count." + if (!isNetworkAvailable) " Сеть недоступна." else ""

        NotificationManagerCompat.from(this).notify(1, notification(text, state))
    }

    private suspend fun sendReportQuery(item: ReportQueryItemEntity) {
        repository.sendReport(item)
    }

    private fun getReportQuery(db: AppDatabase): ReportQueryItemEntity? {
        return db.reportQueryDao().all.firstOrNull()
    }

    private fun sendSendQuery(item: SendQueryItemEntity) {
        val urlConnection = URL(item.url)
        with(urlConnection.openConnection() as HttpURLConnection) {
            requestMethod = "POST"

            val wr = OutputStreamWriter(outputStream)
            wr.write(item.post_data)
            wr.flush()

            if (responseCode != 200) {
                throw Exception("Wrong response code.")
            }
        }
    }

    fun stopTimer() {
        timelimitNotificationStartTime = null
    }

    fun pauseTimer(startTime: Long, endTime: Long) {
        val timeLimitStart = timelimitNotificationStartTime ?: return
        timelimitNotificationStartTime = null
        timeUntilRun = (timeLimitStart + TIMELIMIT_NOTIFICATION_TIMEOUT - System.currentTimeMillis()).toInt()
        pauseDisableJob?.cancel()
        pauseDisableJob = GlobalScope.launch {
            delay((endTime - startTime) * 1000)
            if (!isActive) return@launch
            startTaskClosingTimer(true)
        }
    }

    private fun getSendQuery(db: AppDatabase): SendQueryItemEntity? {
        return db.sendQueryDao().all.firstOrNull()
    }

    override fun onDestroy() {
        thread?.cancel()
        stopForeground(true)
        isRunning = false
        super.onDestroy()
    }

    companion object {
        var isRunning: Boolean = false
        var instance: ReportService? = null
    }
}

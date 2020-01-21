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
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import org.joda.time.DateTime
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity
import ru.relabs.kurjer.persistence.entities.SendQueryItemEntity
import ru.relabs.kurjer.repository.PauseType
import ru.relabs.kurjer.utils.logError
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


const val CHANNEL_ID = "notification_channel"
const val CLOSE_SERVICE_TIMEOUT = 80 * 60 * 1000
const val TIMELIMIT_NOTIFICATION_TIMEOUT = 30 * 60 * 1000

class ReportService : Service() {
    private var thread: Job? = null
    private var currentIconBitmap: Bitmap? = null
    private var lastState: ServiceState = ServiceState.IDLE
    private var lastActivityResumeTime = 0L
    private var lastActivityRunningState = false
    private var lastGPSPending = System.currentTimeMillis()
    private var timelimitNotificationStartTime: Long? = null
    private var timeUntilEnd: Long? = null
    private var pausedUntil: Long? = null
    private var pauseType: PauseType? = null

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
        var lastNetworkEnabledChecking = System.currentTimeMillis()
        var lastServiceLogTime = System.currentTimeMillis()

        thread?.cancel()
        thread = launch {
            while (true) {
                val db = MyApplication.instance.database

                var isTaskSended = false

                if (NetworkHelper.isNetworkAvailable(applicationContext)) {
                    val sendQuery = getSendQuery(db)
                    val reportQuery = getReportQuery(db)

                    if (reportQuery != null) {
                        try {
                            sendReportQuery(db, reportQuery)
                            isTaskSended = true
                            PersistenceHelper.removeReport(db, reportQuery)
                        } catch (e: Exception) {
                            e.logError()
                        }
                    } else if (sendQuery != null) {
                        try {
                            sendSendQuery(sendQuery)
                            isTaskSended = true
                            db.sendQueryDao().delete(sendQuery)
                        } catch (e: Exception) {
                            e.logError()
                        }
                    } else if (System.currentTimeMillis() - lastTasksChecking > 25 * 60 * 1000) {
                        val app = MyApplication.instance
                        if (app.user is UserModel.Authorized) {
                            val user = app.user as UserModel.Authorized
                            val time = DateTime().toString("yyyy-MM-dd'T'HH:mm:ss")
                            try {
                                val tasks = DeliveryServerAPI.api.getTasks(user.token, time).await()
                                if (PersistenceHelper.isMergeNeeded(app.database, tasks.map { it.toTaskModel(app.deviceUUID) })) {
                                    val int = Intent().apply {
                                        putExtra("tasks_changed", true)
                                        action = "NOW"
                                    }
                                    sendBroadcast(int)
                                }
                            } catch (e: Exception) {
                                e.logError()
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
                if (System.currentTimeMillis() - lastServiceLogTime > 30 * 1000) {
                    lastServiceLogTime = System.currentTimeMillis()
                    try {
                        val count = db.sendQueryDao().all.size + db.reportQueryDao().all.size
                    } catch (e: java.lang.Exception) {
                        e.logError()
                    }
                }
                updateNotificationText(db)

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
            MyApplication.instance.requestLocation()
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
            launch(CommonPool) {
                timelimitNotificationStartTime = null
                if (MyApplication.instance.database.taskDao().allOpened.isEmpty()) {
                    return@launch
                }
                withContext(UI) {
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

    fun startTaskClosingTimer() {
        timelimitNotificationStartTime = System.currentTimeMillis()
        timeUntilEnd?.let { untilEnd ->
            timelimitNotificationStartTime?.let { startTime ->
                timelimitNotificationStartTime = startTime + untilEnd
            }
            timeUntilEnd = null
        }
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

    private suspend fun sendReportQuery(db: AppDatabase, item: ReportQueryItemEntity) {
        NetworkHelper.sendReport(
                item,
                db.photosDao().getByTaskItemId(item.taskItemId)
        )
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

    fun pauseTimer(pauseType: PauseType, endTime: Long) {
        val startTime = timelimitNotificationStartTime
        startTime ?: return

        timeUntilEnd = startTime + TIMELIMIT_NOTIFICATION_TIMEOUT - System.currentTimeMillis()
        timelimitNotificationStartTime = null
        pausedUntil = endTime
        this.pauseType = pauseType
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

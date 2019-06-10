package ru.relabs.kurjer

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.joda.time.DateTime
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity
import ru.relabs.kurjer.persistence.entities.SendQueryItemEntity
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


class ReportService : Service() {
    private var thread: Job? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun notification(body: String): Notification {
        val channelId = "notification_channel"
        val pi = PendingIntent.getService(this, 0, Intent(this, ReportService::class.java).apply { putExtra("stopService", true) }, PendingIntent.FLAG_CANCEL_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(notificationChannel)
        }

        return NotificationCompat.Builder(applicationContext)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_arrow)
                .setWhen(System.currentTimeMillis())
                .addAction(R.drawable.ic_stop_black_24dp, "Отключить", pi)
                .setChannelId(channelId)
                .setOnlyAlertOnce(true)
                .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.extras?.getBoolean("stopService") == true) {
            stopSelf()
            return START_STICKY
        }

        startForeground(1, notification("Сервис отправки данных."))

        val db = MyApplication.instance.database
        var lastTasksChecking = System.currentTimeMillis()
        var lastNetworkEnabledChecking = System.currentTimeMillis()

        thread = launch {
            while (true) {
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
                    updateNotificationText(db = db)
                }else{
                    updateNotificationText(text = "Сеть недоступна")
                }

                if(System.currentTimeMillis() - lastNetworkEnabledChecking > 10*60*1000){
                    lastNetworkEnabledChecking = System.currentTimeMillis()
                    if(!NetworkHelper.isNetworkEnabled(applicationContext)){
                        val int = Intent().apply {
                            putExtra("network_disabled", true)
                            action = "NOW"
                        }
                        sendBroadcast(int)
                    }
                }

                delay(if (!isTaskSended) 1000 else 100)
            }
        }

        return START_STICKY
    }

    private fun updateNotificationText(text: String? = null, db: AppDatabase? = null) {
        when {
            text != null -> startForeground(1, notification("Сервис отправки данных. $text"))
            db != null -> {
                val count = db.sendQueryDao().all.size + db.reportQueryDao().all.size
                startForeground(1, notification("Сервис отправки данных. В очереди $count"))
            }
            else -> startForeground(1, notification("Сервис отправки данных."))
        }
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

    private fun getSendQuery(db: AppDatabase): SendQueryItemEntity? {
        return db.sendQueryDao().all.firstOrNull()
    }

    override fun onDestroy() {
        thread?.cancel()
        stopForeground(true)
        super.onDestroy()
    }
}

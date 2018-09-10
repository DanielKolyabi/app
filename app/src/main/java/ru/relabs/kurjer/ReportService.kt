package ru.relabs.kurjer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity
import ru.relabs.kurjer.persistence.entities.SendQueryItemEntity
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


class ReportService : Service() {
    private lateinit var thread: Job
    private lateinit var notification: Notification

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun notification(body: String): Notification {
        val pi = PendingIntent.getService(this, 0, Intent(this, ReportService::class.java).apply { putExtra("stopService", true) }, PendingIntent.FLAG_CANCEL_CURRENT)
        return Notification.Builder(applicationContext)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setWhen(System.currentTimeMillis())
                .addAction(R.drawable.ic_stop_black_24dp, "Отключить", pi)
                .build();
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.extras?.getBoolean("stopService") == true) {
            stopSelf()
            return Service.START_STICKY
        }

        startForeground(1, notification("Сервис отправки данных."))

        val db = (application as MyApplication).database

        thread = launch {
            while (true) {
                var isTaskSended = false
                if (NetworkHelper.isNetworkAvailable(applicationContext)) {
                    val sendQuery = getSendQuery(db)
                    if (sendQuery != null) {
                        try {
                            sendSendQuery(sendQuery)
                            isTaskSended = true
                            db.sendQueryDao().delete(sendQuery)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        getReportQuery(db)?.let {
                            try {
                                sendReportQuery(db, it)
                                isTaskSended = true
                                PersistenceHelper.removeReport(db, it)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                return@let
                            }
                        }
                    }
                }
                updateNotificationText(db)
                delay(if (!isTaskSended) 10000 else 1000)
            }
        }

        return Service.START_STICKY
    }

    private fun updateNotificationText(db: AppDatabase) {
        val count = db.sendQueryDao().all.size + db.reportQueryDao().all.size
        startForeground(1, notification("Сервис отправки данных. В очереди $count"))
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
        val urlConnection = URL(item.url).openConnection() as HttpURLConnection
        urlConnection.apply {
            requestMethod = "POST"
            doInput = true
            doOutput = true
        }
        val stream = BufferedOutputStream(urlConnection.outputStream)
        BufferedWriter(OutputStreamWriter(stream, "UTF-8")).apply {
            write(item.post_data)
            flush()
            close()
        }
        stream.close()
        urlConnection.connect()
    }

    private fun getSendQuery(db: AppDatabase): SendQueryItemEntity? {
        return db.sendQueryDao().all.firstOrNull()
    }

    override fun onDestroy() {
        thread.cancel()
        stopForeground(true)
        super.onDestroy()
    }
}

package ru.relabs.kurjer

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast

class ReportService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        Log.d("report service", "Service Binded")
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Служба запущена", Toast.LENGTH_LONG).show()

        val notification = Notification.Builder(applicationContext)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("testet")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setWhen(System.currentTimeMillis())
                .build();

        startForeground(1, notification)

        return Service.START_STICKY
    }
}

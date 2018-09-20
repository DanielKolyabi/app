package ru.relabs.kurjer

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import ru.relabs.kurjer.files.PathHelper

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide();

        startService(Intent(this, ReportService::class.java))

        AsyncTask.execute {
            Thread.sleep(2000)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }




    override fun onBackPressed() {
        return
    }
}

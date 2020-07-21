package ru.relabs.kurjer

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()

        startService(Intent(this, ReportService::class.java))

        AsyncTask.execute {
            GlobalScope.launch {
                MyApplication.instance.pauseRepository.loadPauseDurations()
                Thread.sleep(500)
                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }


    override fun onBackPressed() {
        return
    }
}

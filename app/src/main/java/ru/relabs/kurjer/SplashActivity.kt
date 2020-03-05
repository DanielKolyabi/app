package ru.relabs.kurjer

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Window
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()

        startService(Intent(this, ReportService::class.java))

        AsyncTask.execute {
            launch {
                MyApplication.instance.pauseRepository.loadPauseDurations()
                Thread.sleep(500)
                withContext(UI) {
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

package ru.relabs.kurjer

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Window
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.domain.repositories.PauseRepository

class SplashActivity : AppCompatActivity() {
    val pauseRepository: PauseRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()

        startService(Intent(this, ReportService::class.java))

        AsyncTask.execute {
            GlobalScope.launch {
                pauseRepository.loadPauseDurations()
                delay(500)
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

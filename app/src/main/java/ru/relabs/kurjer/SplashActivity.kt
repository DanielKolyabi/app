package ru.relabs.kurjer

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Window
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.presentation.host.HostActivity

class SplashActivity : AppCompatActivity() {
    val scope = CoroutineScope(Dispatchers.Default)
    val pauseRepository: PauseRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()

        startService(Intent(this, ReportService::class.java))

        scope.launch {
            pauseRepository.loadPauseDurations()
            withContext(Dispatchers.Main) {
                startActivity(HostActivity.getIntent(this@SplashActivity))
                finish()
            }
        }
    }


    override fun onBackPressed() {
        return
    }
}

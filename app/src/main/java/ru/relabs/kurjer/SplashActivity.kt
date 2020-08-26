package ru.relabs.kurjer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.presentation.host.HostActivity
import ru.relabs.kurjer.services.ReportService

class SplashActivity : AppCompatActivity() {
    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + supervisor)
    private val pauseRepository: PauseRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        startService(Intent(this, ReportService::class.java))

        scope.launch(Dispatchers.IO) {
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

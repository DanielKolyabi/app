package ru.relabs.kurjer

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity
import android.view.Window
import android.view.WindowManager
import kotlinx.coroutines.*
import ru.relabs.kurjer.databinding.AlertActivityBinding
import ru.relabs.kurjer.services.ReportService

/**
 * Created by Daniil Kurchanov on 06.08.2019.
 */
class AlertNotificationActivity: AppCompatActivity() {
    private var notificationMediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        val binding = AlertActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        super.onCreate(savedInstanceState)

        enableNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
        }

        val job = GlobalScope.launch(Dispatchers.Default) {
            delay(30000)
            if(!isActive){
                return@launch
            }
            onCanceled()
            finish()
        }

        binding.okButton.setOnClickListener {
            disableNotification()
            job.cancel()
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onCanceled()
    }

    fun onCanceled(){
        disableNotification()
        ReportService.restartTaskClosingTimerSync()
    }

    fun enableNotification(){
        val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        v?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(1000L, 250L), 0))
            } else {
                v.vibrate(longArrayOf(1000L, 250L), 0)
            }
        }
        notificationMediaPlayer = MediaPlayer.create(applicationContext, R.raw.notification_sound)
        notificationMediaPlayer?.isLooping = true
        notificationMediaPlayer?.start()

    }

    fun disableNotification(){
        (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
        notificationMediaPlayer?.stop()
    }
}
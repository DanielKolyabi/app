package ru.relabs.kurjer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import android.view.Window
import android.view.WindowManager
import kotlinx.android.synthetic.main.alert_activity.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

/**
 * Created by Daniil Kurchanov on 06.08.2019.
 */
class AlertNotificationActivity: AppCompatActivity() {
    private var notificationMediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        setContentView(R.layout.alert_activity)
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

        val job = launch(CommonPool) {
            delay(30000)
            if(!isActive){
                return@launch
            }
            onCanceled()
            finish()
        }

        ok_button.setOnClickListener {
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
        startService(Intent(this@AlertNotificationActivity, ReportService::class.java).apply { putExtra("start_closing_timer", true) })
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
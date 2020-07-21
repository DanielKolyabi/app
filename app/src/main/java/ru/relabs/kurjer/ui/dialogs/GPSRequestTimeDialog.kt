package ru.relabs.kurjer.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import kotlinx.android.synthetic.main.dialog_timer_loading.*
import kotlinx.coroutines.*
import ru.relabs.kurjer.R

/**
 * Created by Daniil Kurchanov on 04.12.2019.
 */
class GPSRequestTimeDialog(ctx: Context, val cancelable: Boolean = false) :
    Dialog(ctx, R.style.Theme_AppCompat_Dialog) {
    var progress = 0
    var timerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (!cancelable) {
            setCancelable(false)
        }
        setContentView(R.layout.dialog_timer_loading)

        pb_loader?.progress = 0
        pb_loader?.max = 400
        pb_loader?.isIndeterminate = false
        tv_progress?.visibility = View.VISIBLE
        tv_progress?.text = "Ищём вас по GPS"

        timerJob = GlobalScope.launch{
            while(isActive){
                delay(100)
                progress++
                withContext(Dispatchers.Main){
                    pb_loader?.progress = progress
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        timerJob?.cancel()
    }
}
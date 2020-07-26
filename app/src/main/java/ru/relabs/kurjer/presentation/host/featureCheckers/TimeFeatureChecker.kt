package ru.relabs.kurjer.presentation.host.featureCheckers

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import com.instacart.library.truetime.TrueTime
import org.joda.time.DateTime
import ru.relabs.kurjer.R
import ru.relabs.kurjer.utils.extensions.showDialog
import ru.relabs.kurjer.utils.log
import kotlin.math.abs

class TimeFeatureChecker(a: Activity) : FeatureChecker(a) {
    private var dialogShowed: Boolean = false

    override fun isFeatureEnabled(): Boolean {
        return try {
            abs(TrueTime.now().time - DateTime.now().millis) < 10 * 60 * 1000
        } catch (e: Exception) {
            true
        }
    }

    override fun requestFeature() {
        if (dialogShowed) return
        activity?.let {
            dialogShowed = true
            it.showDialog(
                R.string.time_request_valid,
                R.string.settings to {
                    dialogShowed = false
                    val intent = Intent(Settings.ACTION_DATE_SETTINGS)
                    try {
                        activity?.startActivity(intent)
                    } catch (x: Exception) {
                        x.log()
                    }
                }
            )
        }
    }
}
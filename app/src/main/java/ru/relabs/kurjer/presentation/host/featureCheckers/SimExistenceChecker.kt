package ru.relabs.kurjer.presentation.host.featureCheckers

import android.app.Activity
import ru.relabs.kurjer.R
import ru.relabs.kurjer.utils.NetworkHelper
import ru.relabs.kurjer.utils.extensions.showDialog

class SimExistenceChecker(a: Activity) : FeatureChecker(a) {
    private var requestShowed = false

    override fun isFeatureEnabled(): Boolean {
//        return NetworkHelper.isSIMInserted(activity)
        return true
    }

    override fun requestFeature() {
        if (requestShowed) return
        requestShowed = true
        activity?.showDialog(
            R.string.sim_required_error,
            R.string.ok to {
                requestShowed = false
                if (!isFeatureEnabled()) {
                    requestFeature()
                }
            }
        )
    }
}
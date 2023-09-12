package ru.relabs.kurjer.presentation.host.featureCheckers

import android.app.Activity
import ru.relabs.kurjer.R
import ru.relabs.kurjer.utils.NetworkHelper
import ru.relabs.kurjer.utils.extensions.showDialog

class NetworkFeatureChecker(a: Activity) : FeatureChecker(a) {
    private var requestShowed = false

    override fun isFeatureEnabled(): Boolean {
        return NetworkHelper.isNetworkEnabled(activity)
    }

    override fun requestFeature() {
        if (requestShowed) return
        requestShowed = true
        activity?.showDialog(
            R.string.network_request_enable,
            R.string.ok to {
                requestShowed = false
                if (!isFeatureEnabled()) {
                    requestFeature()
                }
            }
        )
    }
}
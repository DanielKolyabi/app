package ru.relabs.kurjer.presentation.host.featureCheckers

import android.app.Activity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.providers.ConnectivityProvider
import ru.relabs.kurjer.utils.NetworkHelper
import ru.relabs.kurjer.utils.extensions.showDialog

class NetworkFeatureChecker(a: Activity) : FeatureChecker(a), KoinComponent {
    private var requestShowed = false
    private val connectivityProvider: ConnectivityProvider by inject()

    override fun isFeatureEnabled(): Boolean {
        val status = NetworkHelper.isNetworkEnabled(activity)
        connectivityProvider.setStatus(status)
        return status
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
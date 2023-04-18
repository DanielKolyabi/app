package ru.relabs.kurjer.presentation.host.featureCheckers

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.providers.LocationProvider
import ru.relabs.kurjer.utils.NetworkHelper
import ru.relabs.kurjer.utils.extensions.showDialog

class MockedLocationChecker(
    val a: Activity,
    val locationProvider: LocationProvider,
    val scope: CoroutineScope
) : FeatureChecker(a) {
    private var requestShowed = false
    private var isMockedLocationFound = false

    init {
        scope.launch(Dispatchers.IO) {
            locationProvider.location.collect {
                if(it?.isFromMockProvider == true){
                   isMockedLocationFound = true
                }
            }
        }
    }

    fun reset() {
        isMockedLocationFound = false
    }

    override fun isFeatureEnabled(): Boolean {
        return !isMockedLocationFound
    }

    override fun requestFeature() {
        if (requestShowed) return
        requestShowed = true
        activity?.showDialog(
            R.string.mocked_location_disable_request,
            R.string.ok to {
                requestShowed = false
                isMockedLocationFound = false
                a.finish()
            }
        )
    }
}
package ru.relabs.kurjer.presentation.host.featureCheckers

import android.app.Activity
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import ru.relabs.kurjer.utils.NetworkHelper
import ru.relabs.kurjer.utils.debug

class GPSFeatureChecker(a: Activity) : FeatureChecker(a) {
    private var requestShowed = false

    override fun isFeatureEnabled(): Boolean {
        return NetworkHelper.isGPSEnabled(activity)
    }

    override fun requestFeature() {
        showLocationRequest()
    }

    private fun showLocationRequest() {
        if(requestShowed) return

        val a = activity ?: return
        val googleApiClient = GoogleApiClient.Builder(a)
            .addApi(LocationServices.API).build()
        googleApiClient.connect()

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 30000
        locationRequest.fastestInterval = 15000
        locationRequest.smallestDisplacement = 10f

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        requestShowed = true

        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build()).setResultCallback { result ->
            val status = result.status
            requestShowed = false

            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS ->
                    debug("All location settings are satisfied.")

                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    debug("Location settings are not satisfied. Show the user a dialog to upgrade location settings ")
                    status.startResolutionForResult(activity, 999)
                }

                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->
                    debug("Location settings are inadequate, and cannot be fixed here. Dialog not created.")
            }
        }
    }
}
package ru.relabs.kurjer.presentation.host.featureCheckers

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import ru.relabs.kurjer.domain.providers.LocationProvider

class FeatureCheckersContainer(
    a: Activity,
    val locationProvider: LocationProvider,
    val scope: CoroutineScope
) {

    val gps = GPSFeatureChecker(a)
    val network = NetworkFeatureChecker(a)
    val xiaomiPermissions = XiaomiPermissionsFeatureChecker(a)
    val permissions = PermissionFeatureChecker(a)
    val time = TimeFeatureChecker(a)
    val mockLocation = MockedLocationChecker(a, locationProvider, scope)
    val sim = SimExistenceChecker(a)

    private val allFeatures: List<FeatureChecker>
        get() = listOf(gps, network, xiaomiPermissions, permissions, time, mockLocation)

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        allFeatures.forEach {
            it.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        allFeatures.forEach {
            it.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun onDestroy() {
        allFeatures.forEach { it.onDestroy() }
    }
}
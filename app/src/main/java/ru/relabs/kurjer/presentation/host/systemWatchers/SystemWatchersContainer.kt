package ru.relabs.kurjer.presentation.host.systemWatchers

import android.app.Activity
import ru.relabs.kurjer.presentation.host.featureCheckers.ExternalStoragePermissionFC
import ru.relabs.kurjer.presentation.host.featureCheckers.GPSFeatureChecker
import ru.relabs.kurjer.presentation.host.featureCheckers.MockedLocationChecker
import ru.relabs.kurjer.presentation.host.featureCheckers.NetworkFeatureChecker
import ru.relabs.kurjer.presentation.host.featureCheckers.SimExistenceChecker

class SystemWatchersContainer(
    activity: Activity,
    networkFeatureChecker: NetworkFeatureChecker,
    gpsFeatureChecker: GPSFeatureChecker,
    mockedLocationChecker: MockedLocationChecker,
    simFeatureChecker: SimExistenceChecker,
    externalStorageFC: ExternalStoragePermissionFC
) {
    private val gps = GPSSystemWatcher(activity, gpsFeatureChecker)
    private val network = NetworkSystemWatcher(activity, networkFeatureChecker)
    private val sim = SimExistenceWatcher(activity, simFeatureChecker)
    val mockedLocation = MockedLocationWatcher(activity, mockedLocationChecker)
    private val externalStorage = ExternalStoragePermissionWatcher(activity, externalStorageFC).apply {  }

    private val allWatchers = listOf(
        gps,
        network,
        mockedLocation,
        sim,
        externalStorage
    )

    fun onPause() {
        allWatchers.forEach { it.onPause() }
    }

    fun onResume() {
        allWatchers.forEach { it.onResume() }
    }

    fun onDestroy() {
        allWatchers.forEach { it.onDestroy() }
    }
}
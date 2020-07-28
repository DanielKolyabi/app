package ru.relabs.kurjer.presentation.host.systemWatchers

import android.app.Activity
import android.app.Application
import android.os.Bundle
import ru.relabs.kurjer.presentation.host.featureCheckers.GPSFeatureChecker
import ru.relabs.kurjer.presentation.host.featureCheckers.NetworkFeatureChecker

class SystemWatchersContainer(
    activity: Activity,
    networkFeatureChecker: NetworkFeatureChecker,
    gpsFeatureChecker: GPSFeatureChecker
) {
    private val gps = GPSSystemWatcher(activity, gpsFeatureChecker)
    private val network = NetworkSystemWatcher(activity, networkFeatureChecker)

    private val allWatchers = listOf(
        gps,
        network
    )

    fun onPause(){
        allWatchers.forEach { it.onPause() }
    }

    fun onResume(){
        allWatchers.forEach { it.onResume() }
    }

    fun onDestroy(){
        allWatchers.forEach { it.onDestroy() }
    }
}
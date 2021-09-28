package ru.relabs.kurjer.presentation.host.systemWatchers

import android.app.Activity
import android.app.Application
import android.os.Bundle
import ru.relabs.kurjer.presentation.host.featureCheckers.GPSFeatureChecker
import ru.relabs.kurjer.presentation.host.featureCheckers.MockedLocationChecker
import ru.relabs.kurjer.presentation.host.featureCheckers.NetworkFeatureChecker
import ru.relabs.kurjer.presentation.host.featureCheckers.SimExistenceChecker

class SystemWatchersContainer(
    activity: Activity,
    networkFeatureChecker: NetworkFeatureChecker,
    gpsFeatureChecker: GPSFeatureChecker,
    mockedLocationChecker: MockedLocationChecker,
    simFeatureChecker: SimExistenceChecker
) {
    private val gps = GPSSystemWatcher(activity, gpsFeatureChecker)
    private val network = NetworkSystemWatcher(activity, networkFeatureChecker)
    private val sim = SimExistenceWatcher(activity, simFeatureChecker)
    val mockedLocation = MockedLocationWatcher(activity, mockedLocationChecker)

    private val allWatchers = listOf(
        gps,
        network,
        mockedLocation,
        sim
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
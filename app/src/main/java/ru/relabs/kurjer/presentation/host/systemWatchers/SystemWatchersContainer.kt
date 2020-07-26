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

    init {
        activity.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity) {
                allWatchers.forEach { it.onPause() }
            }

            override fun onActivityDestroyed(activity: Activity) {
                allWatchers.forEach { it.onDestroy() }
            }

            override fun onActivityResumed(activity: Activity) {
                allWatchers.forEach { it.onResume() }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        })
    }
}
package ru.relabs.kurjer.presentation.host.systemWatchers

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.presentation.host.featureCheckers.NetworkFeatureChecker
import java.util.concurrent.TimeUnit

class NetworkSystemWatcher(
    a: Activity,
    private val networkFeatureChecker: NetworkFeatureChecker
) : SystemWatcher(a, TimeUnit.SECONDS.toMillis(10)) {

    override suspend fun onWatcherTick() {
        super.onWatcherTick()
        if (!networkFeatureChecker.isFeatureEnabled()) {
            withContext(Dispatchers.Main){
                networkFeatureChecker.requestFeature()
            }
        }
    }
}
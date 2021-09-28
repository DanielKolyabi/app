package ru.relabs.kurjer.presentation.host.systemWatchers

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.presentation.host.featureCheckers.NetworkFeatureChecker
import ru.relabs.kurjer.presentation.host.featureCheckers.SimExistenceChecker
import java.util.concurrent.TimeUnit

class SimExistenceWatcher(
    a: Activity,
    private val simFeatureChecker: SimExistenceChecker
) : SystemWatcher(a, TimeUnit.SECONDS.toMillis(10)) {

    override suspend fun onWatcherTick() {
        super.onWatcherTick()
        if (!simFeatureChecker.isFeatureEnabled()) {
            withContext(Dispatchers.Main){
                simFeatureChecker.requestFeature()
            }
        }
    }
}
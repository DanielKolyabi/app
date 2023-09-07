package ru.relabs.kurjer.presentation.host.systemWatchers

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.presentation.host.featureCheckers.ExternalStoragePermissionFC
import java.util.concurrent.TimeUnit

class ExternalStoragePermissionWatcher(activity: Activity,private val externalStorageFC: ExternalStoragePermissionFC) :
    SystemWatcher(activity, TimeUnit.SECONDS.toMillis(3)) {

    override suspend fun onWatcherTick() {
        super.onWatcherTick()
        if (!externalStorageFC.isFeatureEnabled()){
            withContext(Dispatchers.Main){
                externalStorageFC.requestFeature()
            }
        }
    }
}
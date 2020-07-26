package ru.relabs.kurjer.presentation.host.systemWatchers

import android.app.Activity
import kotlinx.coroutines.*

abstract class SystemWatcher(
    internal var activity: Activity?,
    private val delay: Long = DEFAULT_WATCHER_TICK_DELAY
) {
    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + supervisor)
    private var job: Job? = null

    open fun onResume() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(delay)
                onWatcherTick()
            }
        }
    }

    open fun onPause() {
        job?.cancel()
    }

    internal open suspend fun onWatcherTick() {}

    fun onDestroy() {
        activity = null
        supervisor.cancel()
    }

    companion object {
        const val DEFAULT_WATCHER_TICK_DELAY = 10 * 1000L
    }
}
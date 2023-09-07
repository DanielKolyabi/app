package ru.relabs.kurjer.domain.storage

class AppInitialStorage(private val appPreferences: AppPreferences) {
    val appFirstStarted: Boolean
        get() = appPreferences.getInitialString() == null

    fun putInitialString() = appPreferences.putInitialString()
}
package ru.relabs.kurjer.domain.providers

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.relabs.kurjer.utils.NetworkHelper

class ConnectivityProvider(context: Context) {
    private val _connected = MutableStateFlow(NetworkHelper.isNetworkEnabled(context))
    val connected: StateFlow<Boolean> = _connected

    fun setStatus(status: Boolean) {
        _connected.tryEmit(status)
    }
}
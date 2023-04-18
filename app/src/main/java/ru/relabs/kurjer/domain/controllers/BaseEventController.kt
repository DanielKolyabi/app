package ru.relabs.kurjer.domain.controllers

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

open class BaseEventController<T> {
    private val eventChannel = MutableSharedFlow<T>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    fun subscribe(): Flow<T> = eventChannel.asSharedFlow()

    fun send(event: T): Boolean {
        return eventChannel.tryEmit(event)
    }
}
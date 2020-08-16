package ru.relabs.kurjer.domain.controllers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.drop

@ExperimentalCoroutinesApi
open class BaseEventController<T> {
    private var isDropNeeded: Boolean = false
    private val eventChannel = BroadcastChannel<T>(Channel.CONFLATED)

    //If any event was sent need to remove last event from channel on subscribe.
    //https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-conflated-broadcast-channel/
    // - "Broadcasts the most recently sent element (aka value) to all openSubscription subscribers"
    //But in case of eventBus implementation we don't need recent element right after subscription
    fun subscribe(): Flow<T> = eventChannel.asFlow().let {
        if (isDropNeeded) {
            it.drop(1)
        } else {
            it
        }
    }

    fun send(event: T): Boolean {
        isDropNeeded = true
        return eventChannel.offer(event)
    }
}
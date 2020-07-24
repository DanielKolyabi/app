package ru.relabs.kurjer.presentation.host

import ru.relabs.kurjer.presentation.base.fragment.AppBarSettings
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgState

object HostMessages {
    fun msgInit(restored: Boolean): HostMessage = msgEffects(
        { it },
        {
            listOf(HostEffects.effectInit(restored))
        }
    )

    fun msgUpdateAppBar(settings: AppBarSettings): HostMessage =
        msgState { it.copy(settings = settings) }

    fun msgResume(): HostMessage = msgEffect(HostEffects.effectCheckApiVersion())

    fun msgAddLoaders(i: Int): HostMessage =
        msgState { it.copy(loaders = it.loaders + i) }

    fun msgLogout(): HostMessage =
        msgEffect(HostEffects.effectLogout())

    fun msgCopyDeviceUUID(): HostMessage =
        msgEffect(HostEffects.effectCopyDeviceUUID())
}
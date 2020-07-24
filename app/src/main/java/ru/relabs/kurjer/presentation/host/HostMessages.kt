package ru.relabs.kurjer.presentation.host

import android.net.Uri
import ru.relabs.kurjer.domain.models.AppUpdatesInfo
import ru.relabs.kurjer.presentation.base.fragment.AppBarSettings
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgState
import ru.relabs.kurjer.utils.XiaomiUtilities
import java.net.URL

object HostMessages {
    fun msgInit(restored: Boolean): HostMessage = msgEffects(
        { it },
        {
            listOf(
                HostEffects.effectInit(restored),
                HostEffects.effectCheckUpdates()
            )
        }
    )

    fun msgUpdateAppBar(settings: AppBarSettings): HostMessage =
        msgState { it.copy(settings = settings) }

    fun msgResume(): HostMessage = msgEffects(
        { it },
        { state ->
            listOfNotNull(
                HostEffects.effectCheckXiaomiPermissions()
                    .takeIf { XiaomiUtilities.isMIUI },
                HostEffects.effectCheckGPSEnabled(),
                HostEffects.effectCheckNetworkEnabled(),
                HostEffects.effectCheckTimeValid()
            )
        }
    )

    fun msgAddLoaders(i: Int): HostMessage =
        msgState { it.copy(loaders = it.loaders + i) }

    fun msgLogout(): HostMessage =
        msgEffect(HostEffects.effectLogout())

    fun msgCopyDeviceUUID(): HostMessage =
        msgEffect(HostEffects.effectCopyDeviceUUID())

    fun msgStartUpdateLoading(url: Uri): HostMessage =
        msgEffect(HostEffects.effectLoadUpdate(url))

    fun msgLoadProgress(progress: Int?): HostMessage =
        msgState { it.copy(loadProgress = progress) }

    fun msgRequestUpdates(): HostMessage =
        msgEffect(HostEffects.effectCheckUpdates())

    fun msgUpdatesInfo(value: AppUpdatesInfo): HostMessage =
        msgState { it.copy() }
}
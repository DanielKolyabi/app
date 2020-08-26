package ru.relabs.kurjer.presentation.host

import android.net.Uri
import ru.relabs.kurjer.domain.models.AppUpdatesInfo
import ru.relabs.kurjer.domain.repositories.PauseType
import ru.relabs.kurjer.presentation.base.fragment.AppBarSettings
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgState
import java.io.File

object HostMessages {
    fun msgInit(restored: Boolean): HostMessage = msgEffects(
        { it },
        {
            listOf(
                HostEffects.effectInit(restored),
                HostEffects.effectSubscribe()
            )
        }
    )

    fun msgUpdateAppBar(settings: AppBarSettings): HostMessage =
        msgState { it.copy(settings = settings) }

    fun msgResume(): HostMessage = msgEffects(
        { it },
        { state ->
            listOfNotNull(
                HostEffects.effectCheckRequirements(), //Updates, Xiaomi Permissions, GPS, Network, Time
                HostEffects.effectEnableLocation()
            )
        }
    )

    fun msgPause(): HostMessage = msgEffects(
        { it },
        { listOf(HostEffects.effectDisableLocation()) }
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
        msgState { it.copy(updateLoadProgress = progress) }

    fun msgRequestUpdates(): HostMessage =
        msgEffects(
            { it },
            { state ->
                listOfNotNull(
                    HostEffects.effectCheckUpdates()
                        .takeIf { !state.isUpdateDialogShowed }
                )
            }
        )

    fun msgUpdatesInfo(value: AppUpdatesInfo): HostMessage =
        msgState { it.copy(appUpdates = value) }

    fun msgUpdateLoadingFailed(): HostMessage =
        msgState { it.copy(isUpdateLoadingFailed = true) }

    fun msgUpdateLoaded(file: File): HostMessage =
        msgState { it.copy(updateFile = file) }

    fun msgUpdateDialogShowed(b: Boolean): HostMessage =
        msgState { it.copy(isUpdateDialogShowed = b) }

    fun msgPauseClicked(): HostMessage =
        msgEffect(HostEffects.effectEnablePause())

    fun msgPauseStart(type: PauseType): HostMessage =
        msgEffect(HostEffects.effectPauseStart(type))

    fun msgIsPaused(paused: Boolean): HostMessage =
        msgState { it.copy(isPaused = paused) }

    fun msgRequiredUpdateOk(): HostMessage =
        msgEffect(HostEffects.effectNavigateUpdateTaskList())

    fun msgRequiredUpdateLater(): HostMessage =
        msgEffect(HostEffects.effectNotifyUpdateRequiredOnTasksOpen())
}
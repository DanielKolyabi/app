package ru.relabs.kurjer.presentation.host

import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.controllers.ServiceEvent
import ru.relabs.kurjer.domain.controllers.TaskEvent
import ru.relabs.kurjer.domain.providers.FirebaseToken
import ru.relabs.kurjer.domain.repositories.PauseType
import ru.relabs.kurjer.domain.useCases.AppUpdateUseCase
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.host.featureCheckers.FeatureChecker
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.extensions.getFirebaseToken
import java.io.File

object HostEffects {
    fun effectInit(restored: Boolean): HostEffect = { c, _ ->
        if (!restored) {
            if (c.repository.isAuthenticated() && c.loginUseCase.isAutologinEnabled()) {
                c.loginUseCase.autoLogin()
                withContext(Dispatchers.Main) {
                    c.router.newRootScreen(RootScreen.tasks(true))
                }
                withContext(Dispatchers.IO) {
                    when (val result = FirebaseMessaging.getInstance().getFirebaseToken()) {
                        is Right -> c.repository.updatePushToken(FirebaseToken(result.value))
                        is Left -> FirebaseCrashlytics.getInstance().log("Can't get firebase token")
                        else -> {}
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    c.router.newRootScreen(RootScreen.login())
                }
            }
        }
        withContext(Dispatchers.IO) {
            c.repository.updateSavedData()
        }
    }

    fun effectLogout(): HostEffect = { c, s ->
        c.loginUseCase.logout()
        withContext(Dispatchers.Main) {
            c.router.newRootScreen(RootScreen.login())
        }
    }

    fun effectEnableLocation(): HostEffect = { c, s ->
        withContext(Dispatchers.Main) {
            if (c.featureCheckersContainer?.gps?.isFeatureEnabled() == true && c.featureCheckersContainer?.permissions?.isFeatureEnabled() == true) {
                if (!c.locationProvider.startInBackground()) {
                    CustomLog.writeToFile("Unable to launch gps in background")
                }
            }
        }
    }

    fun effectDisableLocation(): HostEffect = { c, s ->
        withContext(Dispatchers.Main) {
            if (c.featureCheckersContainer?.gps?.isFeatureEnabled() == true) {
                c.locationProvider.stopInBackground()
            }
        }
    }

    //UPDATES
    fun effectCheckUpdates(): HostEffect = { c, s ->
        messages.send(HostMessages.msgAddLoaders(1))
        when (val r = s.appUpdates?.let { Right(it) } ?: c.updatesUseCase.getAppUpdatesInfo()) {
            is Right -> {
                messages.send(HostMessages.msgUpdatesInfo(r.value))
                if (r.value.required != null && r.value.required.version > BuildConfig.VERSION_CODE) {
                    withContext(Dispatchers.Main) {
                        if (c.showUpdateDialog(r.value.required)) {
                            messages.send(HostMessages.msgUpdateDialogShowed(true))
                        }
                    }
                } else if (r.value.optional != null && r.value.optional.version > BuildConfig.VERSION_CODE) {
                    withContext(Dispatchers.Main) {
                        if (c.showUpdateDialog(r.value.optional)) {
                            messages.send(HostMessages.msgUpdateDialogShowed(true))
                        }
                    }
                }
            }

            is Left -> withContext(Dispatchers.Main) {
                c.repository.updateNetworkStatus(false)
                c.showErrorDialog(R.string.update_cant_get_info)
            }

            else -> {}
        }
        messages.send(HostMessages.msgAddLoaders(-1))
    }

    fun effectLoadUpdate(uri: Uri): HostEffect = { c, s ->
        messages.send(HostMessages.msgLoadProgress(0))
        c.updatesUseCase.downloadUpdate(uri).collect {
            when (it) {
                is AppUpdateUseCase.DownloadState.Progress ->
                    messages.send(HostMessages.msgLoadProgress(((it.current / it.total.toFloat()) * 100).toInt()))

                is AppUpdateUseCase.DownloadState.Success -> {
                    messages.send(HostMessages.msgUpdateLoaded(it.file))
                    messages.send(msgEffect(effectInstallUpdate(it.file)))
                }

                AppUpdateUseCase.DownloadState.Failed -> {
                    messages.send(HostMessages.msgUpdateLoadingFailed())
                    withContext(Dispatchers.Main) {
                        c.showErrorDialog(R.string.update_install_error)
                    }
                }

                else -> {}
            }
        }
        messages.send(HostMessages.msgLoadProgress(null))
    }

    fun effectInstallUpdate(file: File): HostEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.installUpdate(file)
        }
    }

    suspend fun checkFeature(check: FeatureChecker): Boolean {
        if (!check.isFeatureEnabled()) {
            withContext(Dispatchers.Main) {
                check.requestFeature()
            }
            return false
        }
        return true
    }

    fun effectCheckRequirements(): HostEffect = { c, s ->
        val featureCheckersContainer = c.featureCheckersContainer
        if (featureCheckersContainer != null) {
            if (
                checkFeature(featureCheckersContainer.permissions) &&
                checkFeature(featureCheckersContainer.xiaomiPermissions) &&
                checkFeature(featureCheckersContainer.network) &&
                checkFeature(featureCheckersContainer.gps) &&
                checkFeature(featureCheckersContainer.time) &&
                checkFeature(featureCheckersContainer.externalStorage)
            ) {

                if (isUpdateRequired(s) &&
                    s.updateFile == null &&
                    !s.isUpdateDialogShowed &&
                    s.updateLoadProgress == null
                ) {

                    messages.send(msgEffect(effectCheckUpdates()))
                } else if (s.updateFile != null && isUpdateRequired(s)) {
                    messages.send(msgEffect(effectInstallUpdate(s.updateFile)))
                }
            }
        }
    }

    private fun isUpdateRequired(state: HostState): Boolean =
        state.appUpdates == null || ((state.appUpdates.required?.version ?: 0) > BuildConfig.VERSION_CODE && !state.isUpdateLoadingFailed)

    fun effectPauseStart(type: PauseType): HostEffect = { c, s ->
        messages.send(HostMessages.msgAddLoaders(1))
        if (!c.pauseRepository.isPauseAvailable(type)) {
            withContext(Dispatchers.Main) {
                c.showErrorDialog(R.string.error_pause_unavailable)
            }
        } else {
            if (!c.pauseRepository.isPauseAvailableRemote(type)) {
                withContext(Dispatchers.Main) {
                    c.showErrorDialog(R.string.error_pause_unavailable)
                }
            } else {
                c.pauseRepository.startPause(type)
            }
        }
        messages.send(HostMessages.msgAddLoaders(-1))
    }

    fun effectSubscribe(): HostEffect = { c, s ->
        coroutineScope {
            launch {
                c.taskEventController.subscribe().collect {
                    when (it) {
                        is TaskEvent.TasksUpdateRequired -> withContext(Dispatchers.Main) {
                            if (!it.showDialogInTasks) {
                                c.showTaskUpdateRequired(c.settings.canSkipUpdates)
                            }
                        }

                        is TaskEvent.TaskClosed -> Unit
                        is TaskEvent.TaskItemClosed -> Unit
                        is TaskEvent.TaskStorageClosed -> Unit
                        else -> {}
                    }
                }
            }
            launch {
                while (isActive) {
                    delay(5000)
                    if (!c.updatesUseCase.isAppUpdated) {
                        messages.send(HostMessages.msgRequestUpdates())
                    }
                }
            }
            launch {
                c.serviceEventController.subscribe().collect {
                    if (it == ServiceEvent.Stop) {
                        withContext(Dispatchers.Main) {
                            c.finishApp()
                        }
                    }
                }
            }
        }
    }

    fun effectNavigateUpdateTaskList(): HostEffect = { c, s ->
        withContext(Dispatchers.Main) {
            c.router.newRootScreen(RootScreen.tasks(true))
        }
    }

    fun effectNotifyUpdateRequiredOnTasksOpen(): HostEffect = { c, s ->
        c.taskEventController.send(TaskEvent.TasksUpdateRequired(true))
    }
}
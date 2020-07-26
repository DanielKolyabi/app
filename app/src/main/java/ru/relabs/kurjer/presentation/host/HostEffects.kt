package ru.relabs.kurjer.presentation.host

import android.net.Uri
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.R
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.host.featureCheckers.FeatureChecker
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.debug
import ru.relabs.kurjer.utils.extensions.getFirebaseToken
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object HostEffects {
    fun effectInit(restored: Boolean): HostEffect = { c, _ ->
        if (!restored) {
            if (c.repository.isAuthenticated()) {
                withContext(Dispatchers.Main) {
                    c.router.newRootScreen(RootScreen.Tasks)
                }

                withContext(Dispatchers.IO) {
                    when (val result = FirebaseInstanceId.getInstance().getFirebaseToken()) {
                        is Right -> Unit //TODO: Upload c.repository.updateMe(firebaseToken = result.value)
                        is Left -> Unit//FirebaseCrashlytics.getInstance().log("Can't get firebase token")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    c.router.newRootScreen(RootScreen.Login)
                }
            }
        }
    }

    fun effectLogout(): HostEffect = { c, s ->
        c.loginUseCase.logout()
        withContext(Dispatchers.Main) {
            c.router.newRootScreen(RootScreen.Login)
        }
    }

    fun effectCopyDeviceUUID(): HostEffect = { c, _ ->
        val uuid = c.deviceUUIDProvider.getOrGenerateDeviceUUID()
        withContext(Dispatchers.Main) {
            c.copyToClipboard(uuid.id)
        }
    }

    //UPDATES
    fun effectCheckUpdates(): HostEffect = { c, s ->
        messages.send(HostMessages.msgAddLoaders(1))
        when (val r = s.appUpdates?.let { Right(it) } ?: c.updatesUseCase.getAppUpdatesInfo()) {
            is Right -> {
                messages.send(HostMessages.msgUpdatesInfo(r.value))
                if (r.value.required != null) {
                    withContext(Dispatchers.Main) {
                        if (c.showUpdateDialog(r.value.required)) {
                            messages.send(HostMessages.msgUpdateDialogShowed(true))
                        }
                    }
                } else if (r.value.optional != null) {
                    withContext(Dispatchers.Main) {
                        if (c.showUpdateDialog(r.value.optional)) {
                            messages.send(HostMessages.msgUpdateDialogShowed(true))
                        }
                    }
                }
            }
            is Left -> withContext(Dispatchers.Main) {
                c.showErrorDialog(R.string.update_cant_get_info)
            }

        }
        messages.send(HostMessages.msgAddLoaders(-1))
    }

    fun effectLoadUpdate(uri: Uri): HostEffect = { c, s ->
        messages.send(HostMessages.msgLoadProgress(0))
        val url = URL(uri.toString())
        val file = PathHelper.getUpdateFile()
        val connection = url.openConnection() as HttpURLConnection
        val stream = url.openStream()
        val fos = FileOutputStream(file)
        try {
            val fileSize = connection.contentLength
            val b = ByteArray(2048)
            var read = stream.read(b)
            var total = read
            while (read != -1) {
                fos.write(b, 0, read)
                read = stream.read(b)
                total += read
                debug("Load update file $total/$fileSize")
                messages.send(HostMessages.msgLoadProgress(((total / fileSize.toFloat()) * 100).toInt()))
            }

            messages.send(HostMessages.msgUpdateLoaded(file))
            messages.send(msgEffect(effectInstallUpdate(file)))
        } catch (e: Exception) {
            debug("Loading error", e)
            messages.send(HostMessages.msgUpdateLoadingFailed())
            withContext(Dispatchers.Main) {
                c.showErrorDialog(R.string.update_install_error)
            }
        } finally {
            stream.close()
            connection.disconnect()
            fos.close()
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
                checkFeature(featureCheckersContainer.time)
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
}
package ru.relabs.kurjer.presentation.host

import android.net.Uri
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.presentation.base.tea.msgEffect
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

    fun effectCheckUpdates(): HostEffect = { c, s ->
        messages.send(HostMessages.msgAddLoaders(1))
        when (val r = s.appUpdates?.let { Right(it) } ?: c.updatesUseCase.getAppUpdatesInfo()) {
            is Right -> {
                messages.send(HostMessages.msgUpdatesInfo(r.value))
                if (r.value.required != null) {
                    withContext(Dispatchers.Main) {
                        if(c.showUpdateDialog(r.value.required)){
                            messages.send(HostMessages.msgUpdateDialogShowed(true))
                        }
                    }
                } else if (r.value.optional != null) {
                    withContext(Dispatchers.Main) {
                        if(c.showUpdateDialog(r.value.optional)){
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

    fun effectCheckXiaomiPermissions(): HostEffect = { c, s ->
        //TODO("Check permissions")
    }

    fun effectCheckGPSEnabled(): HostEffect = { c, s ->
        //TODO("Check GPS")
    }

    fun effectCheckNetworkEnabled(): HostEffect = { c, s ->
        //TODO("Check Network")
    }

    fun effectCheckTimeValid(): HostEffect = { c, s ->
        //TODO("Check Time")
    }
}
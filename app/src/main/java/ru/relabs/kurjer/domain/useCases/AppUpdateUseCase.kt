package ru.relabs.kurjer.domain.useCases

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.data.models.common.EitherE
import ru.relabs.kurjer.domain.models.AppUpdatesInfo
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.debug
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateUseCase(
    private val deliveryRepository: DeliveryRepository,
    private val pathsProvider: PathsProvider
) {
    private var requiredAppVersion: Int? = null
    private var updateDownloadingFails: Boolean = false

    val isUpdateUnavailable: Boolean
        get() = updateDownloadingFails

    val isAppUpdated: Boolean
        get() = requiredAppVersion?.let { it < BuildConfig.VERSION_CODE } ?: true

    suspend fun getAppUpdatesInfo(): EitherE<AppUpdatesInfo> {
        val result = deliveryRepository.getAppUpdatesInfo()
        if (result is Right) {
            requiredAppVersion = result.value.required?.version
        }
        return result
    }

    suspend fun downloadUpdate(uri: Uri): Flow<DownloadState> = flow {
        val url = URL(uri.toString())
        val file = pathsProvider.getUpdateFile()
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
                emit(DownloadState.Progress(total, fileSize))
            }
            emit(DownloadState.Success(file))
        } catch (e: Exception) {
            emit(DownloadState.Failed)
            updateDownloadingFails = true
            debug("Loading error", e)
        } finally {
            stream.close()
            connection.disconnect()
            fos.close()
        }
    }.flowOn(Dispatchers.IO)


    sealed class DownloadState {
        data class Progress(val current: Int, val total: Int) : DownloadState()
        object Failed : DownloadState()
        data class Success(val file: File) : DownloadState()
    }
}

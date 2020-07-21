package ru.relabs.kurjer.domain.repositories

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import ru.relabs.kurjer.data.api.DeliveryApi
import ru.relabs.kurjer.data.models.common.ApiError
import ru.relabs.kurjer.data.models.common.DomainException
import ru.relabs.kurjer.data.models.common.EitherE
import ru.relabs.kurjer.data.models.pause.PauseTimeResponse
import ru.relabs.kurjer.data.models.pause.PauseTimesResponse
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.storage.CurrentUserStorage
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.debug
import java.io.File

class DeliveryRepository(
    private val deliveryApi: DeliveryApi,
    private val authTokenStorage: AuthTokenStorage,
    private val currentUserStorage: CurrentUserStorage,
    private val cacheDir: File
) {
    //TODO: Add mapping
    fun isAuthenticated(): Boolean = authTokenStorage.getToken() != null

    //Reports
    fun sendReport(item: ReportQueryItemEntity) {
//        NetworkHelper.sendReport(
//            item,
//            db.photosDao().getByTaskItemId(item.taskItemId)
//        )
        TODO("Not yet implemented")
    }

    //Pauses
    suspend fun getLastPauseTimes(): EitherE<PauseTimeResponse> = authenticatedRequest { token ->
        deliveryApi.getLastPauseTimes(token)
    }

    suspend fun isPauseAllowed(pauseType: PauseType): EitherE<Boolean> = authenticatedRequest { token ->
        deliveryApi.isPauseAllowed(token, pauseType.ordinal).status
    }

    suspend fun getPauseDurations(): EitherE<PauseTimesResponse> = anonymousRequest {
        deliveryApi.getPauseDurations()
    }

    private inline fun <T> authenticatedRequest(block: (token: String) -> T): EitherE<T> {
        return authTokenStorage.getToken()?.let { token -> anonymousRequest { block(token) } }
            ?: Left(DomainException.ApiException(ApiError(401, "Empty token", null)))
    }

    private inline fun <T> anonymousRequest(block: () -> T): EitherE<T> {
        return try {
            Right(block())
        } catch (e: CancellationException) {
            Left(DomainException.CanceledException)
        } catch (e: HttpException) {
            mapApiException(e)?.let { Left(it) } ?: Left(DomainException.UnknownException)
        } catch (e: Exception) {
            debug("UnknownException $e")
            FirebaseCrashlytics.getInstance().recordException(e)
            Left(DomainException.UnknownException)
        }
    }

    private fun mapApiException(httpException: HttpException): DomainException.ApiException? {
        return parseErrorBody(httpException.response())?.let { DomainException.ApiException(it) }
    }

    private fun parseErrorBody(response: Response<*>?): ApiError? {
        return try {
            Gson().fromJson(response?.errorBody()?.string(), ApiError::class.java)
        } catch (e: Exception) {
            debug("Can't parse HTTP error", e)
            return null
        }
    }
}
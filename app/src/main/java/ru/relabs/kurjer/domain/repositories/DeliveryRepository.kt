package ru.relabs.kurjer.domain.repositories

import android.location.Location
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import org.joda.time.DateTime
import retrofit2.HttpException
import retrofit2.Response
import ru.relabs.kurjer.data.api.DeliveryApi
import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.data.models.common.ApiError
import ru.relabs.kurjer.data.models.common.DomainException
import ru.relabs.kurjer.data.models.common.EitherE
import ru.relabs.kurjer.domain.mappers.*
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.domain.providers.DeviceUUIDProvider
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.useCases.LoginUseCase
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.debug
import java.io.File

class DeliveryRepository(
    private val deliveryApi: DeliveryApi,
    private val authTokenStorage: AuthTokenStorage,
    private val loginUseCase: LoginUseCase,
    private val cacheDir: File,
    private val deviceIdProvider: DeviceUUIDProvider
) {
    //TODO: Add mapping
    fun isAuthenticated(): Boolean = authTokenStorage.getToken() != null

    suspend fun login(login: UserLogin, password: String): EitherE<User> = anonymousRequest {
        val r = deliveryApi.login(
            login.login,
            password,
            deviceIdProvider.getOrGenerateDeviceUUID().id,
            currentTime()
        )
        val user = UserMapper.fromRaw(r.user)
        loginUseCase.logIn(user, r.token)

        user
    }

    suspend fun login(token: String): EitherE<User> = anonymousRequest {
        val r = deliveryApi.loginByToken(
            token,
            deviceIdProvider.getOrGenerateDeviceUUID().id,
            currentTime()
        )
        val user = UserMapper.fromRaw(r.user)
        loginUseCase.logIn(user, r.token)

        user
    }

    suspend fun getTasks(): EitherE<List<Task>> = authenticatedRequest { token ->
        val deviceId = deviceIdProvider.getOrGenerateDeviceUUID()
        deliveryApi.getTasks(
            token,
            currentTime()
        ).map {
            TaskMapper.fromRaw(it, deviceId)
        }
    }

    suspend fun getAppUpdatesInfo(): EitherE<AppUpdatesInfo> = anonymousRequest {
        UpdatesMapper.fromRaw(deliveryApi.getUpdateInfo())
    }

    suspend fun updatePushToken(pushToken: String): EitherE<Boolean> = authenticatedRequest { token ->
        deliveryApi.sendPushToken(token, pushToken)
        true
    }

    suspend fun updateDeviceIMEI(imei: String): EitherE<Boolean> = authenticatedRequest { token ->
        deliveryApi.sendDeviceImei(token, imei)
        true
    }

    suspend fun updateLocation(location: Location): EitherE<Boolean> = authenticatedRequest { token ->
        deliveryApi.sendGPS(
            token,
            location.latitude,
            location.longitude,
            currentTime()
        )
        true
    }

    //Reports
    fun sendReport(item: ReportQueryItemEntity) {
//        NetworkHelper.sendReport(
//            item,
//            db.photosDao().getByTaskItemId(item.taskItemId)
//        )
        TODO("Not yet implemented")
    }

    //Pauses
    suspend fun getLastPauseTimes(): EitherE<PauseTimes> = authenticatedRequest { token ->
        PauseMapper.fromRaw(deliveryApi.getLastPauseTimes(token))
    }

    suspend fun isPauseAllowed(pauseType: PauseType): EitherE<Boolean> = authenticatedRequest { token ->
        deliveryApi.isPauseAllowed(token, pauseType.ordinal).status
    }

    suspend fun getPauseDurations(): EitherE<PauseDurations> = anonymousRequest {
        PauseMapper.fromRaw(deliveryApi.getPauseDurations())
    }

    suspend fun getAllowedCloseRadius(): EitherE<AllowedCloseRadius> = authenticatedRequest { token ->
        RadiusMapper.fromRaw(deliveryApi.getRadius(token))
    }

    //Could be sended in other user session
    suspend fun startPause(pauseType: PauseType, token: String, startTime: Long): EitherE<Boolean> = anonymousRequest {
        deliveryApi.startPause(token, pauseType.toInt(), startTime)
        true
    }

    suspend fun stopPause(pauseType: PauseType, token: String, stopTime: Long): EitherE<Boolean> = anonymousRequest {
        deliveryApi.stopPause(token, pauseType.toInt(), stopTime)
        true
    }

    fun currentTime(): String = DateTime().toString("yyyy-MM-dd'T'HH:mm:ss")

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
            if (e.code() == 401) {
                Left(DomainException.ApiException(ApiError(401, "Unauthorized", null)))
            } else {
                mapApiException(e)?.let { Left(it) } ?: Left(DomainException.UnknownException)
            }
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
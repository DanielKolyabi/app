package ru.relabs.kurjer.data.api

import okhttp3.MultipartBody
import retrofit2.http.*
import ru.relabs.kurjer.data.models.*
import ru.relabs.kurjer.data.models.auth.AuthResponse
import ru.relabs.kurjer.data.models.common.StatusResponse
import ru.relabs.kurjer.data.models.pause.PauseTimeResponse
import ru.relabs.kurjer.data.models.pause.PauseTimesResponse
import ru.relabs.kurjer.data.models.radius.RadiusResponse
import ru.relabs.kurjer.data.models.tasks.TaskResponse

interface DeliveryApi {
    @POST("api/v1/auth")
    @FormUrlEncoded
    suspend fun login(
        @Field("login") login: String,
        @Field("password") password: String,
        @Field("device_id") deviceId: String,
        @Field("current_time") currentTime: String
    ): AuthResponse

    @POST("api/v1/auth/token")
    @FormUrlEncoded
    suspend fun loginByToken(
        @Field("token") token: String,
        @Field("device_id") deviceId: String,
        @Field("current_time") currentTime: String
    ): AuthResponse

    @GET("api/v1/tasks")
    suspend fun getTasks(
        @Header("X-TOKEN") token: String,
        @Query("current_time") currentTime: String
    ): List<TaskResponse>

    @POST("api/v1/tasks/{id}/report")
    @Multipart
    suspend fun sendTaskReport(
        @Header("X-TOKEN") token: String,
        @Path("id") taskItemId: Int,
        @Part("data") data: TaskItemReportRequest,
        @Part photos: List<MultipartBody.Part>
    )

    @GET("api/v1/update")
    suspend fun getUpdateInfo(): UpdatesResponse

    @POST("api/v1/push_token")
    suspend fun sendPushToken(
        @Header("X-TOKEN") token: String,
        @Query("push_token") pushToken: String
    )

    @POST("api/v1/device_imei")
    suspend fun sendDeviceImei(
        @Header("X-TOKEN") token: String,
        @Query("device_imei") imei: String
    )

    @POST("api/v1/coords")
    suspend fun sendGPS(
        @Header("X-TOKEN") token: String,
        @Query("lat") lat: Double,
        @Query("long") long: Double,
        @Query("time") time: String
    )

    @GET("api/v1/pause/time")
    suspend fun getPauseDurations(): PauseTimesResponse

    @GET("api/v1/pause/last")
    suspend fun getLastPauseTimes(
        @Header("X-TOKEN") token: String
    ): PauseTimeResponse

    @GET("api/v1/pause/check")
    suspend fun isPauseAllowed(
        @Header("X-TOKEN") token: String,
        @Query("type") pauseType: Int
    ): StatusResponse

    @POST("api/v1/pause/start")
    suspend fun startPause(
        @Header("X-TOKEN") token: String,
        @Query("type") type: Int,
        @Query("time") time: Long
    )

    @POST("api/v1/pause/stop")
    suspend fun stopPause(
        @Header("X-TOKEN") token: String,
        @Query("type") type: Int,
        @Query("time") time: Long
    )

    @GET("api/v1/radius")
    suspend fun getRadius(
        @Header("X-TOKEN") token: String
    ): RadiusResponse
}
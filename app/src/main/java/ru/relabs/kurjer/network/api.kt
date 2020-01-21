package ru.relabs.kurjer.network

import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import kotlinx.coroutines.experimental.Deferred
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.network.models.*
import java.util.concurrent.TimeUnit


/**
 * Created by ProOrange on 23.08.2018.
 */
object DeliveryServerAPI {

    private val interceptor = HttpLoggingInterceptor()

    init {
        interceptor.level = HttpLoggingInterceptor.Level.BASIC
    }

    val timeoutInterceptor = object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()

            if (request.url().toString().matches(Regex(".*/api/v1/tasks/[0-9]*/report.*"))) {
                return chain.withConnectTimeout(5, TimeUnit.SECONDS)
                        .withReadTimeout(120, TimeUnit.SECONDS)
                        .withWriteTimeout(120, TimeUnit.SECONDS)
                        .proceed(request)
            } else if (request.url().toString().matches(Regex(".*/api/v1/tasks.*"))) {
                return chain.withConnectTimeout(5, TimeUnit.SECONDS)
                        .withReadTimeout(7, TimeUnit.MINUTES)
                        .withWriteTimeout(10, TimeUnit.SECONDS)
                        .proceed(request)
            } else {
                return chain.withConnectTimeout(5, TimeUnit.SECONDS)
                        .withReadTimeout(15, TimeUnit.SECONDS)
                        .withWriteTimeout(10, TimeUnit.SECONDS)
                        .proceed(request)
            }
        }
    }

    private val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(timeoutInterceptor)
            .build()

    var gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()

    private val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(client)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    interface IDeliveryServerAPI {
        @POST("api/v1/auth")
        @FormUrlEncoded
        fun login(@Field("login") login: String, @Field("password") password: String, @Field("device_id") deviceId: String, @Field("current_time") currentTime: String): Deferred<AuthResponseModel>

        @POST("api/v1/auth/token")
        @FormUrlEncoded
        fun loginByToken(@Field("token") token: String, @Field("device_id") deviceId: String, @Field("current_time") currentTime: String): Deferred<AuthResponseModel>

        @GET("api/v1/tasks")
        fun getTasks(@Query("token") token: String, @Query("current_time") currentTime: String): Deferred<List<TaskResponseModel>>

        @POST("api/v1/tasks/{id}/report")
        @Multipart
        fun sendTaskReport(@Path("id") taskItemId: Int, @Query("token") token: String, @Part("data") data: TaskItemReportModel, @Part photos: List<MultipartBody.Part>): Deferred<StatusResponse>

        @GET("api/v1/update")
        fun getUpdateInfo(): Deferred<UpdateInfoResponse>

        @POST("api/v1/push_token")
        fun sendPushToken(@Query("token") token: String, @Query("push_token") pushToken: String): Deferred<StatusResponse>

        @POST("api/v1/device_imei")
        fun sendDeviceImei(@Query("token") token: String, @Query("device_imei") imei: String): Deferred<StatusResponse>

        @POST("api/v1/coords")
        fun sendGPS(@Query("token") token: String, @Query("lat") lat: Double, @Query("long") long: Double, @Query("time") time: String): Deferred<StatusResponse>

        @GET("api/v1/pause/time")
        fun getPauseTimes(): Deferred<PauseTimeResponse>

        @GET("api/v1/pause/last")
        fun getLastPauseTimes(@Query("token") token: String): Deferred<PauseTimeResponse>

        @GET("api/v1/pause/check")
        fun isPauseAllowed(@Query("token") token: String, @Query("type") pauseType: Int): Deferred<StatusResponse>

        @POST("api/v1/pause/start")
        fun startPause(@Query("token") token: String, @Query("type") type: Int, @Query("time") time: Int): Deferred<StatusResponse>

        @POST("api/v1/pause/stop")
        fun stopPause(@Query("token") token: String, @Query("type") type: Int, @Query("time") time: Int): Deferred<StatusResponse>

        @GET("api/v1/radius")
        fun getRadius(@Query("token") token: String): Deferred<RadiusResponse>


    }

    val api = retrofit.create(IDeliveryServerAPI::class.java)
}
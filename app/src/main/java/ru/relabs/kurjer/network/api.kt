package ru.relabs.kurjer.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import kotlinx.coroutines.experimental.Deferred
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.network.models.AuthResponseModel
import ru.relabs.kurjer.network.models.TaskResponseModel

/**
 * Created by ProOrange on 23.08.2018.
 */
object DeliveryServerAPI {

    private val interceptor = HttpLoggingInterceptor()
    init {
        interceptor.level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

    private val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(client)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    interface IDeliveryServerAPI {
        @POST("api/v1/auth")
        @FormUrlEncoded
        fun login(@Field("login") login: String, @Field("password") password: String, @Field("device_id") deviceId: String): Deferred<AuthResponseModel>

        @POST("api/v1/auth/token")
        @FormUrlEncoded
        fun loginByToken(@Field("token") token: String, @Field("device_id") deviceId: String): Deferred<AuthResponseModel>

        @GET("api/v1/tasks")
        fun getTasks(@Query("token") token: String): Deferred<List<TaskResponseModel>>
    }

    val api = retrofit.create(IDeliveryServerAPI::class.java)
}
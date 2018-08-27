package ru.relabs.kurjer.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import ru.relabs.kurjer.BuildConfig

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
        @POST("api/v1/login")
        @FormUrlEncoded
        fun login(@Field("login") login: String, @Field("password") password: String, @Field("device_id") deviceId: String): Call<Int>;
    }

    val api = retrofit.create(IDeliveryServerAPI::class.java)
}
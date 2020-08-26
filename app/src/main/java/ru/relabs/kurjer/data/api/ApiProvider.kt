package ru.relabs.kurjer.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import ru.relabs.kurjer.utils.debug
import java.util.concurrent.TimeUnit


class ApiProvider(deliveryUrl: String) {
    val httpClient: OkHttpClient
    val practisApi: DeliveryApi

    private val timeoutInterceptor: Interceptor = object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (request.url.toString().matches(Regex(".*/api/v1/tasks/[0-9]*/report.*"))) {
                return chain.withConnectTimeout(5, TimeUnit.SECONDS)
                    .withReadTimeout(120, TimeUnit.SECONDS)
                    .withWriteTimeout(120, TimeUnit.SECONDS)
                    .proceed(request)
            } else if (request.url.toString().matches(Regex(".*/api/v1/tasks.*"))) {
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

    init {
        httpClient = buildClient()
        practisApi = buildRetrofit(httpClient, deliveryUrl).create()
    }

    private fun buildRetrofit(client: OkHttpClient, baseUrl: String): Retrofit {
        val builder = Retrofit.Builder()
        builder.client(client)
        builder.baseUrl(baseUrl)
        builder.addConverterFactory(GsonConverterFactory.create(buildGson()))
        return builder.build()
    }

    private fun buildGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
    }

    private fun buildClient() = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .addInterceptor(timeoutInterceptor)
        .build()
}
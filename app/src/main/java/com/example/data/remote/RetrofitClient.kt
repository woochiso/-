package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val READ_TIMEOUT_HEADER = "X-Woochiso-Read-Timeout"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val counselingTimeoutSeconds = request.header(READ_TIMEOUT_HEADER)?.toIntOrNull()
            val sanitizedRequest = request.newBuilder().removeHeader(READ_TIMEOUT_HEADER).build()
            if (counselingTimeoutSeconds != null) {
                chain.withReadTimeout(counselingTimeoutSeconds, TimeUnit.SECONDS).proceed(sanitizedRequest)
            } else {
                chain.proceed(sanitizedRequest)
            }
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        // Request bodies contain passwords, so bodies are never logged.
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                )
            }
        }
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.WOOCHISO_API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}

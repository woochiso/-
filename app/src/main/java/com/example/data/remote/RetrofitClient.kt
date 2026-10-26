package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object RetrofitClient {
    private const val READ_TIMEOUT_HEADER = "X-Woochiso-Read-Timeout"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val httpClient = OkHttpClient.Builder()
        .cookieJar(sessionCookies)
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
                // Application interceptor receives OkHttp's transparently decompressed body.
                addInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)
                    if (request.url.encodedPath.contains("/api/meal/")) {
                        val contentType = response.header("Content-Type").orEmpty()
                        val raw = response.peekBody(65536).string()
                        val shape = runCatching { mealJsonShape(raw) }
                            .getOrElse { "unreadable:${it.javaClass.simpleName}" }
                        val screen = mealScreen(request.url.encodedPath)
                        Log.d("MEAL_API", "screen=$screen endpoint=${request.method} ${request.url.encodedPath} status=${response.code} content-type=$contentType json-shape=$shape")
                        if (request.url.encodedPath.endsWith("/meal/save.php") || request.url.encodedPath.endsWith("/meal/update.php")) {
                            Log.d("MEAL_SAVE_RESPONSE", "status=${response.code} content-type=$contentType raw_body=${mealSafePreview(raw)}")
                        }
                        if (!response.isSuccessful || shape.startsWith("non-json")) {
                            Log.e("MEAL_API", "screen=$screen response-preview=${mealSafePreview(raw)}")
                        }
                    }
                    response
                }
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

private fun mealScreen(path: String): String = when {
    path.endsWith("/index.php") || path.endsWith("/detail.php") || path.endsWith("/save.php") || path.endsWith("/update.php") || path.endsWith("/delete.php") -> "record"
    path.endsWith("/favorites.php") || path.endsWith("/quick-record.php") -> "favorites"
    path.endsWith("/foods.php") -> "search"
    path.endsWith("/calendar.php") -> "calendar"
    path.endsWith("/graph.php") -> "graph"
    path.endsWith("/analysis.php") -> "analysis"
    path.endsWith("/analyze.php") || path.endsWith("/nutrition.php") -> "editor"
    path.endsWith("/image.php") -> "image"
    else -> "meal"
}

private fun mealSafePreview(raw: String): String = raw.take(1200)
    .replace(Regex("(?i)Bearer\\s+[A-Za-z0-9._~-]+"), "Bearer [redacted]")
    .replace(Regex("[\\r\\n\\t]+"), " ")

private fun mealJsonShape(raw: String): String {
    fun shape(value: Any?, depth: Int = 0): String = when {
        value == null || value === JSONObject.NULL -> "null"
        depth >= 4 -> value.javaClass.simpleName
        value is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(prefix = "{", postfix = "}") { key -> "$key:${shape(value.opt(key), depth + 1)}" }
        value is JSONArray -> "array(${value.length()})" + if (value.length() > 0) "<${shape(value.opt(0), depth + 1)}>" else ""
        value is String -> "string"
        value is Number -> "number"
        value is Boolean -> "boolean"
        else -> value.javaClass.simpleName
    }
    val trimmed = raw.trim().removePrefix("\uFEFF").trimStart()
    return when {
        trimmed.startsWith("{") -> shape(JSONObject(trimmed)).take(4000)
        trimmed.startsWith("[") -> shape(JSONArray(trimmed)).take(4000)
        else -> "non-json(length=${raw.length})"
    }
}
    private val sessionCookies = object : CookieJar {
        private val cookies = mutableMapOf<String, Cookie>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            synchronized(this) { cookies.forEach { this.cookies[it.name] = it } }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            synchronized(this) {
                val now = System.currentTimeMillis()
                cookies.values.filter { it.expiresAt > now && it.matches(url) }
            }
    }

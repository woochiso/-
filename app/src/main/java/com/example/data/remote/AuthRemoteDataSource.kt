package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.remote.dto.LoginRequest
import com.example.data.remote.dto.LoginResponse
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.json.JSONObject

sealed interface RemoteLoginResult {
    data class Success(val response: LoginResponse) : RemoteLoginResult
    data object InvalidCredentials : RemoteLoginResult
    data object NoInternet : RemoteLoginResult
    data object ServerUnavailable : RemoteLoginResult
    data object InvalidResponse : RemoteLoginResult
    data object ServerError : RemoteLoginResult
}

class AuthRemoteDataSource(
    private val apiService: ApiService
) {
    suspend fun login(email: String, password: String): RemoteLoginResult {
        return try {
            if (BuildConfig.DEBUG) {
                Log.d(
                    AUTH_DEBUG_TAG,
                    "AUTH_REQUEST_START " +
                        "url=${BuildConfig.WOOCHISO_API_BASE_URL}login.php " +
                        "method=POST contentType=application/json " +
                        "jsonKeys=email,password " +
                        "emailLength=${email.length} " +
                        "passwordLength=${password.length} passwordEmpty=${password.isEmpty()}"
                )
            }
            val response = apiService.login(LoginRequest(email = email, password = password))
            val body = response.body()

            if (BuildConfig.DEBUG) {
                val errorJson = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                val parsedError = errorJson?.let {
                    runCatching { JSONObject(it) }.getOrNull()
                }
                val debugSuccess = body?.success ?: parsedError?.optBoolean("success")
                val debugMessage = body?.message ?: parsedError?.optString("message")
                Log.d(
                    AUTH_DEBUG_TAG,
                    "AUTH_RESPONSE " +
                        "status=${response.code()} " +
                        "contentType=${response.headers()["Content-Type"] ?: "unknown"} " +
                        "success=${debugSuccess ?: "unknown"} " +
                        "message=${debugMessage?.take(160) ?: "none"} " +
                        "tokenPresent=${!body?.token.isNullOrBlank()}"
                )
            }

            when {
                response.isSuccessful && body?.success == true &&
                    !body.token.isNullOrBlank() && body.user != null -> RemoteLoginResult.Success(body)
                response.isSuccessful && body == null -> RemoteLoginResult.InvalidResponse
                response.isSuccessful && body?.success == true -> RemoteLoginResult.InvalidResponse
                response.code() >= 500 -> RemoteLoginResult.ServerError
                response.code() == 400 || response.code() == 401 || body?.success == false -> {
                    RemoteLoginResult.InvalidCredentials
                }
                else -> RemoteLoginResult.ServerUnavailable
            }
        } catch (_: JsonDataException) {
            RemoteLoginResult.InvalidResponse
        } catch (_: JsonEncodingException) {
            RemoteLoginResult.InvalidResponse
        } catch (_: UnknownHostException) {
            RemoteLoginResult.NoInternet
        } catch (_: SocketTimeoutException) {
            RemoteLoginResult.ServerUnavailable
        } catch (_: ConnectException) {
            RemoteLoginResult.ServerUnavailable
        } catch (_: IOException) {
            RemoteLoginResult.ServerUnavailable
        } catch (_: Exception) {
            RemoteLoginResult.ServerError
        }
    }

    private companion object {
        const val AUTH_DEBUG_TAG = "WOOCHISO_AUTH_DEBUG"
    }
}

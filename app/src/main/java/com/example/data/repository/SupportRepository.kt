package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.*
import java.io.IOException
import org.json.JSONObject

sealed interface SupportResult<out T> {
    data class Success<T>(val value: T, val message: String? = null) : SupportResult<T>
    data object Unauthorized : SupportResult<Nothing>
    data class Failure(val message: String) : SupportResult<Nothing>
}

class SupportRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun search(query: String): SupportResult<SupportSearchResponse> = publicRequest {
        apiService.searchSupport(query)
    }

    suspend fun notices(query: String?, page: Int): SupportResult<SupportNoticesResponse> = publicRequest {
        apiService.getSupportNotices(query, page)
    }

    suspend fun notice(id: Long): SupportResult<SupportNoticeResponse> = publicRequest {
        apiService.getSupportNotice(id)
    }

    suspend fun faqs(category: String?, query: String?): SupportResult<SupportFaqsResponse> = publicRequest {
        apiService.getSupportFaqs(category, query)
    }

    suspend fun inquiries(): SupportResult<SupportInquiriesResponse> = authenticatedRequest { authorization ->
        apiService.getSupportInquiries(authorization)
    }

    suspend fun inquiry(id: Long): SupportResult<SupportInquiryResponse> = authenticatedRequest { authorization ->
        apiService.getSupportInquiry(authorization, id)
    }

    suspend fun createInquiry(request: SupportInquiryCreateRequest): SupportResult<SupportInquiryResponse> =
        authenticatedRequest { authorization -> apiService.createSupportInquiry(authorization, request) }

    private suspend fun <T> publicRequest(block: suspend () -> retrofit2.Response<T>): SupportResult<T> = execute(block)

    private suspend fun <T> authenticatedRequest(
        block: suspend (String) -> retrofit2.Response<T>
    ): SupportResult<T> {
        val token = tokenManager.accessToken()
        if (token.isNullOrBlank()) return SupportResult.Unauthorized
        return execute { block("Bearer $token") }
    }

    private suspend fun <T> execute(block: suspend () -> retrofit2.Response<T>): SupportResult<T> = try {
        val response = block()
        if (response.isSuccessful && response.body() != null) {
            SupportResult.Success(response.body()!!)
        } else if (response.code() == 401) {
            SupportResult.Unauthorized
        } else {
            SupportResult.Failure(response.errorBody()?.string()?.let(::safeMessage) ?: "내용을 불러오지 못했습니다.")
        }
    } catch (_: IOException) {
        SupportResult.Failure("인터넷 연결을 확인해주세요.")
    } catch (_: Exception) {
        SupportResult.Failure("내용을 불러오지 못했습니다.")
    }

    private fun safeMessage(json: String): String? = runCatching {
        JSONObject(json).optString("message").takeIf(String::isNotBlank)
    }.getOrNull()
}

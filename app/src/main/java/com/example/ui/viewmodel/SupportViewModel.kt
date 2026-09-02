package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.*
import com.example.data.repository.SupportRepository
import com.example.data.repository.SupportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupportUiState(
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val requiresLogin: Boolean = false,
    val searchQuery: String = "",
    val searchNotices: List<SupportNotice> = emptyList(),
    val searchFaqs: List<SupportFaq> = emptyList(),
    val notices: List<SupportNotice> = emptyList(),
    val noticePage: Int = 1,
    val noticePages: Int = 1,
    val selectedNotice: SupportNotice? = null,
    val faqs: List<SupportFaq> = emptyList(),
    val faqCategories: List<SupportOption> = emptyList(),
    val selectedFaqCategory: String? = null,
    val faqQuery: String = "",
    val inquiries: List<SupportInquiry> = emptyList(),
    val inquiryTypes: List<SupportOption> = emptyList(),
    val selectedInquiry: SupportInquiry? = null,
    val createdInquiryId: Long? = null
)

class SupportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SupportRepository(RetrofitClient.apiService, TokenManager(application))
    private val _state = MutableStateFlow(SupportUiState())
    val state: StateFlow<SupportUiState> = _state.asStateFlow()

    fun search(query: String) = launchLoad {
        _state.update { it.copy(searchQuery = query.trim()) }
        apply(repository.search(query.trim())) { response ->
            _state.update { it.copy(searchNotices = response.notices, searchFaqs = response.faqs) }
        }
    }

    fun loadNotices(query: String? = null, page: Int = 1) = launchLoad {
        apply(repository.notices(query?.trim()?.takeIf(String::isNotEmpty), page)) { response ->
            _state.update { it.copy(notices = response.items, noticePage = response.page, noticePages = response.totalPages) }
        }
    }

    fun loadNotice(id: Long) = launchLoad {
        apply(repository.notice(id)) { response -> _state.update { it.copy(selectedNotice = response.notice) } }
    }

    fun loadFaqs(category: String? = null, query: String? = null) = launchLoad {
        _state.update { it.copy(selectedFaqCategory = category, faqQuery = query.orEmpty()) }
        apply(repository.faqs(category, query?.trim()?.takeIf(String::isNotEmpty))) { response ->
            _state.update { it.copy(faqs = response.items, faqCategories = response.categories) }
        }
    }

    fun loadInquiries() = launchLoad {
        apply(repository.inquiries()) { response ->
            _state.update { it.copy(inquiries = response.items, inquiryTypes = response.types) }
        }
    }

    fun loadInquiry(id: Long) = launchLoad {
        apply(repository.inquiry(id)) { response -> _state.update { it.copy(selectedInquiry = response.inquiry) } }
    }

    fun createInquiry(request: SupportInquiryCreateRequest) {
        if (_state.value.submitting) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null, message = null, createdInquiryId = null) }
            when (val result = repository.createInquiry(request)) {
                is SupportResult.Success -> {
                    val response = result.value
                    if (response.success && response.inquiry != null) {
                        _state.update { current -> current.copy(
                            submitting = false,
                            inquiries = listOf(response.inquiry) + current.inquiries.filterNot { it.inquiryId == response.inquiry.inquiryId },
                            message = response.message ?: "문의가 등록되었습니다.",
                            createdInquiryId = response.inquiry.inquiryId
                        ) }
                    } else fail(response.message ?: "문의를 등록하지 못했습니다. 잠시 후 다시 시도해주세요.")
                }
                SupportResult.Unauthorized -> unauthorized()
                is SupportResult.Failure -> fail(result.message)
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null, createdInquiryId = null) }
    fun clearError() = _state.update { it.copy(error = null) }

    private fun launchLoad(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            block()
            _state.update { it.copy(loading = false) }
        }
    }

    private inline fun <T> apply(result: SupportResult<T>, success: (T) -> Unit) {
        when (result) {
            is SupportResult.Success -> success(result.value)
            SupportResult.Unauthorized -> unauthorized()
            is SupportResult.Failure -> fail(result.message)
        }
    }

    private fun unauthorized() = _state.update { it.copy(loading = false, submitting = false, requiresLogin = true, error = "로그인이 만료되었습니다. 다시 로그인해주세요.") }
    private fun fail(message: String) = _state.update { it.copy(loading = false, submitting = false, error = message) }
}

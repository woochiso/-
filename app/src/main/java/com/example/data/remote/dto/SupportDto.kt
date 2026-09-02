package com.example.data.remote.dto

data class SupportNotice(
    val noticeId: Long,
    val title: String,
    val content: String? = null,
    val writerName: String = "우치소",
    val pinned: Boolean = false,
    val viewCount: Long = 0,
    val createdAt: String,
    val updatedAt: String? = null
)

data class SupportFaq(
    val faqId: Long,
    val category: String,
    val categoryLabel: String,
    val question: String,
    val answer: String
)

data class SupportOption(val code: String, val label: String)

data class SupportInquiry(
    val inquiryId: Long,
    val inquiryType: String,
    val inquiryTypeLabel: String,
    val title: String,
    val content: String? = null,
    val status: String,
    val statusLabel: String,
    val adminAnswer: String? = null,
    val createdAt: String,
    val answeredAt: String? = null
)

data class SupportSearchResponse(
    val success: Boolean = false,
    val query: String = "",
    val notices: List<SupportNotice> = emptyList(),
    val faqs: List<SupportFaq> = emptyList(),
    val message: String? = null
)

data class SupportNoticesResponse(
    val success: Boolean = false,
    val items: List<SupportNotice> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val message: String? = null
)

data class SupportNoticeResponse(
    val success: Boolean = false,
    val notice: SupportNotice? = null,
    val message: String? = null
)

data class SupportFaqsResponse(
    val success: Boolean = false,
    val items: List<SupportFaq> = emptyList(),
    val categories: List<SupportOption> = emptyList(),
    val message: String? = null
)

data class SupportInquiriesResponse(
    val success: Boolean = false,
    val items: List<SupportInquiry> = emptyList(),
    val types: List<SupportOption> = emptyList(),
    val message: String? = null
)

data class SupportInquiryResponse(
    val success: Boolean = false,
    val inquiry: SupportInquiry? = null,
    val message: String? = null
)

data class SupportInquiryCreateRequest(
    val inquiryType: String,
    val title: String,
    val content: String
)

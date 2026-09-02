package com.example.ui.screens

import android.text.Html
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.dto.*
import com.example.ui.theme.AppActionButton
import com.example.ui.viewmodel.SupportUiState
import com.example.ui.viewmodel.SupportViewModel

private enum class SupportDestination { HOME, NOTICES, NOTICE, FAQ, INQUIRIES, NEW_INQUIRY, INQUIRY }

@Composable
fun CustomerCenterScreen(
    viewModel: SupportViewModel,
    onAuthExpired: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var destination by remember { mutableStateOf(SupportDestination.HOME) }
    val navigateBack: () -> Unit = {
        when (destination) {
            SupportDestination.HOME -> onExit()
            SupportDestination.NOTICE -> destination = SupportDestination.NOTICES
            SupportDestination.NEW_INQUIRY, SupportDestination.INQUIRY -> destination = SupportDestination.INQUIRIES
            else -> destination = SupportDestination.HOME
        }
        viewModel.clearError()
    }

    BackHandler(onBack = navigateBack)
    LaunchedEffect(state.requiresLogin) { if (state.requiresLogin) onAuthExpired() }
    LaunchedEffect(state.createdInquiryId) {
        state.createdInquiryId?.let {
            viewModel.loadInquiry(it)
            destination = SupportDestination.INQUIRY
            viewModel.consumeMessage()
        }
    }

    Box(modifier.fillMaxSize()) {
        when (destination) {
            SupportDestination.HOME -> SupportHome(
                state,
                onSearch = viewModel::search,
                onNotices = { viewModel.loadNotices(); destination = SupportDestination.NOTICES },
                onFaq = { viewModel.loadFaqs(); destination = SupportDestination.FAQ },
                onInquiries = { viewModel.loadInquiries(); destination = SupportDestination.INQUIRIES },
                onNotice = { viewModel.loadNotice(it); destination = SupportDestination.NOTICE },
                onBack = navigateBack
            )
            SupportDestination.NOTICES -> NoticeList(state, viewModel::loadNotices, navigateBack) {
                viewModel.loadNotice(it); destination = SupportDestination.NOTICE
            }
            SupportDestination.NOTICE -> NoticeDetail(state.selectedNotice, navigateBack)
            SupportDestination.FAQ -> FaqScreen(state, viewModel::loadFaqs, navigateBack)
            SupportDestination.INQUIRIES -> InquiryList(
                state,
                onRetry = viewModel::loadInquiries,
                onNew = { destination = SupportDestination.NEW_INQUIRY },
                onOpen = { viewModel.loadInquiry(it); destination = SupportDestination.INQUIRY },
                onBack = navigateBack
            )
            SupportDestination.NEW_INQUIRY -> InquiryForm(state, viewModel::createInquiry, navigateBack)
            SupportDestination.INQUIRY -> InquiryDetail(state.selectedInquiry, navigateBack)
        }
        if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = AppActionButton)
    }
}

@Composable
private fun SupportHome(
    state: SupportUiState,
    onSearch: (String) -> Unit,
    onNotices: () -> Unit,
    onFaq: () -> Unit,
    onInquiries: () -> Unit,
    onNotice: (Long) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenTitle("고객센터", "서비스 이용 중 궁금한 점이나 불편한 사항을 확인하고 문의할 수 있습니다.", onBack) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(100) },
                placeholder = { Text("궁금한 내용을 검색해보세요.") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { IconButton({ focus.clearFocus(); onSearch(query) }) { Icon(Icons.Default.ArrowForward, "검색") } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); onSearch(query) }),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { SupportMenuCard(Icons.Default.Campaign, "공지사항", "우치소의 새로운 소식을 확인하세요.", onNotices) }
        item { SupportMenuCard(Icons.Default.Lightbulb, "자주 묻는 질문 FAQ", "서비스별 이용 방법을 확인하세요.", onFaq) }
        item { SupportMenuCard(Icons.Default.ChatBubble, "1:1 문의", "궁금한 내용을 직접 문의하세요.", onInquiries) }
        state.error?.let { item { ErrorNotice(it) { onSearch(query) } } }
        if (state.searchQuery.isNotBlank()) {
            item { SectionHeader("“${state.searchQuery}” 검색 결과", state.searchNotices.size + state.searchFaqs.size) }
            items(state.searchNotices, key = { "notice-${it.noticeId}" }) { notice ->
                SearchResultCard("공지사항", notice.title) { onNotice(notice.noticeId) }
            }
            items(state.searchFaqs, key = { "faq-${it.faqId}" }) { faq ->
                SearchResultCard("FAQ · ${faq.categoryLabel}", faq.question, plainText(faq.answer)) { onFaq() }
            }
            if (state.searchNotices.isEmpty() && state.searchFaqs.isEmpty() && !state.loading) item { EmptyText("검색 결과가 없습니다.") }
        }
        item { MentalHealthNotice() }
    }
}

@Composable private fun NoticeList(state: SupportUiState, onLoad: (String?, Int) -> Unit, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    var query by remember { mutableStateOf("") }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle("공지사항", "우치소의 새로운 소식과 이용 안내입니다.", onBack) }
        item { SearchField(query, { query = it.take(100) }, "공지사항 검색") { onLoad(query, 1) } }
        state.error?.let { item { ErrorNotice(it) { onLoad(query, state.noticePage) } } }
        items(state.notices, key = { it.noticeId }) { notice ->
            Card(
                Modifier.fillMaxWidth().clickable { onOpen(notice.noticeId) },
                RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) { Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (notice.pinned) Text("중요", color = AppActionButton, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    Text(notice.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null)
                }
                Text(notice.createdAt.take(10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
        if (state.notices.isEmpty() && !state.loading && state.error == null) item { EmptyText("등록된 공지사항이 없습니다.") }
        if (state.noticePages > 1) item {
            Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                TextButton({ onLoad(query, state.noticePage - 1) }, enabled = state.noticePage > 1) { Text("이전") }
                Text("${state.noticePage} / ${state.noticePages}")
                TextButton({ onLoad(query, state.noticePage + 1) }, enabled = state.noticePage < state.noticePages) { Text("다음") }
            }
        }
        item { MentalHealthNotice() }
    }
}

@Composable private fun NoticeDetail(notice: SupportNotice?, onBack: () -> Unit) = LazyColumn(
    modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    item { ScreenTitle("공지사항", "우치소의 새로운 소식과 이용 안내입니다.", onBack) }
    notice?.let { item {
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(Color.White)) {
            Column(Modifier.padding(18.dp), Arrangement.spacedBy(10.dp)) {
                if (it.pinned) Text("중요 공지", color = AppActionButton, fontWeight = FontWeight.Bold)
                Text(it.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${it.createdAt.take(10)} · ${it.writerName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Text(plainText(it.content.orEmpty()), style = MaterialTheme.typography.bodyLarge)
            }
        }
    } }
}

@Composable private fun FaqScreen(state: SupportUiState, onLoad: (String?, String?) -> Unit, onBack: () -> Unit) {
    var query by remember { mutableStateOf(state.faqQuery) }
    var openId by remember { mutableStateOf<Long?>(null) }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle("자주 묻는 질문 FAQ", "서비스 이용 중 자주 묻는 내용을 모았습니다.", onBack) }
        item { SearchField(query, { query = it.take(100) }, "FAQ 검색") { onLoad(state.selectedFaqCategory, query) } }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(state.selectedFaqCategory == null, { onLoad(null, query) }, { Text("전체") }) }
                items(state.faqCategories, key = { it.code }) { option ->
                    FilterChip(state.selectedFaqCategory == option.code, { onLoad(option.code, query) }, { Text(option.label) })
                }
            }
        }
        state.error?.let { item { ErrorNotice(it) { onLoad(state.selectedFaqCategory, query) } } }
        items(state.faqs, key = { it.faqId }) { faq ->
            Card(
                Modifier.fillMaxWidth().clickable { openId = if (openId == faq.faqId) null else faq.faqId },
                RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(Color.White)
            ) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(8.dp)) {
                Text(faq.categoryLabel, color = AppActionButton, style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Q. ${faq.question}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(if (openId == faq.faqId) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
                if (openId == faq.faqId) { HorizontalDivider(); Text("A. ${plainText(faq.answer)}") }
            } }
        }
        if (state.faqs.isEmpty() && !state.loading && state.error == null) item { EmptyText(if (query.isBlank()) "등록된 FAQ가 없습니다." else "검색 결과가 없습니다.") }
        item { MentalHealthNotice() }
    }
}

@Composable private fun InquiryList(state: SupportUiState, onRetry: () -> Unit, onNew: () -> Unit, onOpen: (Long) -> Unit, onBack: () -> Unit) = LazyColumn(
    modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
) {
    item { ScreenTitle("1:1 문의", "서비스 이용 중 궁금한 점이나 불편한 사항을 남겨주세요.", onBack) }
    item { Button(onNew, colors = ButtonDefaults.buttonColors(AppActionButton, Color.White), modifier = Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("문의하기") } }
    state.error?.let { item { ErrorNotice(it, onRetry) } }
    items(state.inquiries, key = { it.inquiryId }) { inquiry ->
        Card(Modifier.fillMaxWidth().clickable { onOpen(inquiry.inquiryId) }, RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(Color.White)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(inquiry.statusLabel, color = AppActionButton, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text(inquiry.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(inquiry.createdAt.take(10), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, null)
            }
        }
    }
    if (state.inquiries.isEmpty() && !state.loading && state.error == null) item { EmptyText("등록한 문의가 없습니다.") }
}

@Composable private fun InquiryForm(state: SupportUiState, onSubmit: (SupportInquiryCreateRequest) -> Unit, onBack: () -> Unit) {
    var type by remember(state.inquiryTypes) { mutableStateOf(state.inquiryTypes.firstOrNull()?.code.orEmpty()) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier = Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("문의하기", "궁금한 내용을 작성해 주세요.", onBack) }
        item { SupportDropdown("문의 유형", type, state.inquiryTypes) { type = it } }
        item { OutlinedTextField(title, { title = it.take(200); localError = null }, label = { Text("문의 제목") }, placeholder = { Text("제목을 입력해주세요.") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), supportingText = { Text("${title.length}/200") }) }
        item { OutlinedTextField(content, { content = it.take(5000); localError = null }, label = { Text("문의 내용") }, placeholder = { Text("문의 내용을 입력해주세요.") }, minLines = 7, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), supportingText = { Text("${content.length}/5000") }) }
        (localError ?: state.error)?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Button(
            onClick = {
                localError = when { type.isBlank() -> "문의 유형을 선택해주세요."; title.trim().isEmpty() -> "제목을 입력해주세요."; content.trim().isEmpty() -> "문의 내용을 입력해주세요."; else -> null }
                if (localError == null) onSubmit(SupportInquiryCreateRequest(type, title.trim(), content.trim()))
            },
            enabled = !state.submitting,
            colors = ButtonDefaults.buttonColors(AppActionButton, Color.White),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { if (state.submitting) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White) else Text("문의 등록", fontWeight = FontWeight.Bold) } }
    }
}

@Composable private fun InquiryDetail(inquiry: SupportInquiry?, onBack: () -> Unit) = LazyColumn(
    modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    item { ScreenTitle("문의 상세", "등록한 문의와 관리자 답변을 확인합니다.", onBack) }
    inquiry?.let { item {
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(Color.White)) {
            Column(Modifier.padding(18.dp), Arrangement.spacedBy(9.dp)) {
                Text("${it.inquiryTypeLabel} · ${it.statusLabel}", color = AppActionButton, fontWeight = FontWeight.Bold)
                Text(it.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(it.createdAt.take(16), color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(); Text("내 문의 내용", fontWeight = FontWeight.Bold); Text(it.content.orEmpty())
                HorizontalDivider(); Text("관리자 답변", fontWeight = FontWeight.Bold)
                if (it.adminAnswer.isNullOrBlank()) Text("아직 답변이 등록되지 않았습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else { Text(it.adminAnswer); it.answeredAt?.let { date -> Text(date.take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
        }
    } }
}

@Composable private fun ScreenTitle(title: String, description: String, onBack: (() -> Unit)? = null) = Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
        onBack?.let {
            IconButton(onClick = it, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, "이전 화면")
            }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(6.dp)); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun SupportMenuCard(icon: ImageVector, title: String, description: String, onClick: () -> Unit) = Card(
    Modifier.fillMaxWidth().clickable(onClick = onClick), RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(Color.White)
) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
    Surface(color = AppActionButton.copy(alpha = .14f), shape = RoundedCornerShape(12.dp)) { Icon(icon, null, tint = AppActionButton, modifier = Modifier.padding(10.dp).size(25.dp)) }
    Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Default.ChevronRight, null)
} }

@Composable private fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, search: () -> Unit) = OutlinedTextField(
    value, onChange, placeholder = { Text(placeholder) }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { IconButton(search) { Icon(Icons.Default.ArrowForward, "검색") } }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
)

@Composable private fun SearchResultCard(label: String, title: String, description: String? = null, onClick: () -> Unit) = Card(
    Modifier.fillMaxWidth().clickable(onClick = onClick), RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(Color.White)
) { Column(Modifier.padding(14.dp)) { Text(label, color = AppActionButton, style = MaterialTheme.typography.labelMedium); Text(title, fontWeight = FontWeight.SemiBold); description?.let { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } } }

@Composable private fun SectionHeader(title: String, count: Int) = Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.Bold); Text("${count}건", color = AppActionButton) }
@Composable private fun EmptyText(text: String) = Text(text, modifier = Modifier.fillMaxWidth().padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
@Composable private fun ErrorNotice(message: String, retry: () -> Unit) = Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer); TextButton(retry) { Text("다시 시도") } } }

@Composable private fun MentalHealthNotice() = Surface(color = AppActionButton.copy(alpha = .10f), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(7.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.HealthAndSafety, null, tint = AppActionButton); Spacer(Modifier.width(8.dp)); Text("마음 건강 안내", fontWeight = FontWeight.Bold) }; Text("우치소의 AI 기능은 의료적 진단이나 치료를 대신하지 않습니다."); Text("심한 우울감, 자해·자살 생각, 현실적인 위험 등이 있는 경우 전문 의료기관이나 관련 전문기관의 도움을 받아주세요.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun SupportDropdown(label: String, selectedCode: String, options: List<SupportOption>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.code == selectedCode }?.label.orEmpty()
    Box { OutlinedTextField(selected, {}, readOnly = true, label = { Text(label) }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }, modifier = Modifier.fillMaxWidth().clickable { expanded = true }); DropdownMenu(expanded, { expanded = false }) { options.forEach { option -> DropdownMenuItem({ Text(option.label) }, { onSelect(option.code); expanded = false }) } } }
}

private fun plainText(html: String): String = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()

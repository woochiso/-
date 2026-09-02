package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.remote.dto.ProfileUpdateRequest
import com.example.data.remote.dto.ProfileUser
import com.example.ui.theme.AppActionButton
import com.example.ui.viewmodel.ProfileUiState
import java.time.Year

@Composable
fun EditProfileScreen(
    state: ProfileUiState,
    onLoad: () -> Unit,
    onSave: (ProfileUpdateRequest) -> Unit,
    showPageTitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { onLoad() }
    val user = state.user
    var phone by remember(user) { mutableStateOf(user?.phone.orEmpty()) }
    var nickname by remember(user) { mutableStateOf(user?.nickname.orEmpty()) }
    var birthYear by remember(user) { mutableStateOf(user?.birthYear?.toString().orEmpty()) }
    var gender by remember(user) { mutableStateOf(genderLabel(user?.gender)) }
    var occupation by remember(user) { mutableStateOf(user?.occupation.orEmpty()) }
    var maritalStatus by remember(user) { mutableStateOf(maritalLabel(user?.maritalStatus)) }
    var children by remember(user) { mutableStateOf((user?.children ?: 0).coerceIn(0, 5)) }
    val trimmedNickname = nickname.trim()
    val nicknameIsValid = remember(trimmedNickname) {
        NICKNAME_PATTERN.matches(trimmedNickname)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (showPageTitle) Text("회원정보 수정", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (showPageTitle) Spacer(Modifier.height(6.dp))
            Text("내 계정의 기본 정보를 확인하고 수정하세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.isLoading && user == null) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp)) { CircularProgressIndicator() } }
            return@LazyColumn
        }
        state.error?.let { message -> item { StatusNotice(message, true) } }
        state.message?.let { message -> item { StatusNotice(message, false) } }
        if (user == null) return@LazyColumn

        item { ReadOnlyProfileField("이메일", user.email) }
        item {
            OutlinedTextField(phone, { phone = it.take(13) }, label = { Text("휴대폰 번호") },
                placeholder = { Text("010-1234-5678") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(nickname, { nickname = it }, label = { Text("닉네임") },
                supportingText = {
                    Text(if (nickname.isEmpty() || nicknameIsValid) "한글, 영문, 숫자, 공백, 밑줄 2~20자" else "닉네임은 허용된 문자로 2~20자 입력해주세요.")
                },
                isError = nickname.isNotEmpty() && !nicknameIsValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = true,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        }
        item { ReadOnlyProfileField("가입일", user.createdAt.orEmpty()) }
        item { ReadOnlyProfileField("회원등급", user.role.orEmpty()) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(birthYear, { birthYear = it.filter(Char::isDigit).take(4) },
                    label = { Text("출생년도") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
                ProfileDropdown("성별", gender, listOf("남성", "여성", "선택 안 함"), { gender = it }, Modifier.weight(1f))
            }
        }
        item {
            OutlinedTextField(occupation, { occupation = it.take(100) }, label = { Text("직업") },
                supportingText = { Text("입력하지 않아도 저장할 수 있습니다.") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        }
        item {
            ProfileDropdown("결혼 여부", maritalStatus, listOf("미혼", "기혼", "이혼", "사별", "기타"),
                { maritalStatus = it }, Modifier.fillMaxWidth())
        }
        item {
            ProfileDropdown("자녀 수", "${children}명", (0..5).map { "${it}명" },
                { children = it.removeSuffix("명").toInt() }, Modifier.fillMaxWidth())
        }
        item {
            Button(
                onClick = {
                    val year = birthYear.toIntOrNull()
                    onSave(ProfileUpdateRequest(
                        nickname = trimmedNickname, phone = phone.trim(), birthYear = year,
                        gender = genderValue(gender), occupation = occupation.trim(),
                        maritalStatus = maritalValue(maritalStatus), children = children
                    ))
                },
                enabled = !state.isSaving && nicknameIsValid &&
                    (birthYear.isBlank() || birthYear.toIntOrNull() in 1900..Year.now().value),
                colors = ButtonDefaults.buttonColors(containerColor = AppActionButton, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { if (state.isSaving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White) else Text("정보 저장", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun StatusNotice(message: String, error: Boolean) = Surface(
    color = if (error) MaterialTheme.colorScheme.errorContainer else Color(0xFFEAF2FB),
    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
) { Text(message, modifier = Modifier.padding(12.dp), color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface) }

@Composable private fun ReadOnlyProfileField(label: String, value: String) = OutlinedTextField(
    value = value.ifBlank { "정보 없음" }, onValueChange = {}, readOnly = true, label = { Text(label) }, singleLine = true,
    shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)), modifier = Modifier.fillMaxWidth())

@Composable private fun ProfileDropdown(label: String, value: String, options: List<String>, onSelected: (String) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(value, {}, readOnly = true, label = { Text(label) }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        Box(Modifier.fillMaxWidth().height(56.dp).clickable { expanded = true })
        DropdownMenu(expanded, { expanded = false }) { options.forEach { option ->
            DropdownMenuItem({ Text(option) }, { onSelected(option); expanded = false })
        } }
    }
}

private fun genderLabel(value: String?) = when (value) { "MALE" -> "남성"; "FEMALE" -> "여성"; else -> "선택 안 함" }
private fun genderValue(label: String) = when (label) { "남성" -> "MALE"; "여성" -> "FEMALE"; else -> "NO_ANSWER" }
private fun maritalLabel(value: String?) = when (value) { "SINGLE" -> "미혼"; "MARRIED" -> "기혼"; "DIVORCED" -> "이혼"; "BEREAVED" -> "사별"; else -> "기타" }
private fun maritalValue(label: String) = when (label) { "미혼" -> "SINGLE"; "기혼" -> "MARRIED"; "이혼" -> "DIVORCED"; "사별" -> "BEREAVED"; else -> "OTHER" }

private val NICKNAME_PATTERN = Regex("^[가-힣A-Za-z0-9 _]{2,20}$")

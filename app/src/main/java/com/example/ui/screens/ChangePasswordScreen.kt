package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.remote.dto.PasswordChangeRequest
import com.example.ui.theme.AppActionButton
import com.example.ui.viewmodel.PasswordChangeUiState

private const val PASSWORD_SPECIALS = "!@#$%^&*()_-+={}[]:;,.?/~"

@Composable
fun ChangePasswordScreen(
    state: PasswordChangeUiState,
    onSubmit: (PasswordChangeRequest) -> Unit,
    onSuccess: () -> Unit,
    showPageTitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.message) {
        if (state.message != null) onSuccess()
    }

    fun submit() {
        localError = when {
            currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() ->
                "모든 비밀번호를 입력해주세요."
            newPassword != confirmPassword -> "새 비밀번호가 일치하지 않습니다."
            currentPassword == newPassword -> "현재 비밀번호와 다른 비밀번호를 입력해주세요."
            !passwordPolicySatisfied(newPassword) -> "비밀번호 조건을 확인해주세요."
            else -> null
        }
        if (localError == null) {
            focusManager.clearFocus()
            onSubmit(PasswordChangeRequest(currentPassword, newPassword, confirmPassword))
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (showPageTitle) Text("비밀번호 변경", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (showPageTitle) Spacer(Modifier.height(6.dp))
            Text("현재 비밀번호를 확인한 뒤 새 비밀번호로 변경하세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { PasswordField("현재 비밀번호", currentPassword, { currentPassword = it; localError = null }, currentVisible, { currentVisible = !currentVisible }, ImeAction.Next) }
        item { PasswordField("새 비밀번호", newPassword, { newPassword = it.take(64); localError = null }, newVisible, { newVisible = !newVisible }, ImeAction.Next) }
        item { PasswordField("새 비밀번호 확인", confirmPassword, { confirmPassword = it.take(64); localError = null }, confirmVisible, { confirmVisible = !confirmVisible }, ImeAction.Done, { submit() }) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("비밀번호 조건", fontWeight = FontWeight.SemiBold)
                PasswordRule("8~15자", newPassword.length in 8..15)
                PasswordRule("영문 포함", newPassword.any { it in 'A'..'Z' || it in 'a'..'z' })
                PasswordRule("숫자 포함", newPassword.any { it in '0'..'9' })
                PasswordRule("특수문자 포함 ($PASSWORD_SPECIALS)", newPassword.any { it in PASSWORD_SPECIALS })
                PasswordRule("공백 없음", newPassword.isNotEmpty() && newPassword.none(Char::isWhitespace))
            }
        }
        (localError ?: state.error)?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        item {
            Button(
                onClick = ::submit,
                enabled = !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = AppActionButton, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.isSubmitting) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                else Text("비밀번호 변경", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    imeAction: ImeAction,
    onDone: (() -> Unit)? = null
) = OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    singleLine = true,
    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
    keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
    trailingIcon = {
        IconButton(onClick = onToggleVisibility) {
            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (visible) "비밀번호 숨기기" else "비밀번호 보기")
        }
    },
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth()
)

@Composable
private fun PasswordRule(text: String, satisfied: Boolean) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(Icons.Default.CheckCircle, null, tint = if (satisfied) AppActionButton else MaterialTheme.colorScheme.outline)
        Text(text, color = if (satisfied) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun passwordPolicySatisfied(password: String): Boolean =
    password.length in 8..15 &&
        password.any { it in 'A'..'Z' || it in 'a'..'z' } &&
        password.any { it in '0'..'9' } &&
        password.any { it in PASSWORD_SPECIALS } &&
        password.all { it.isAsciiLetterOrDigit() || it in PASSWORD_SPECIALS }

private fun Char.isAsciiLetterOrDigit(): Boolean = this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9'

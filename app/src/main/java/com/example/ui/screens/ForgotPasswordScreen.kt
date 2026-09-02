package com.example.ui.screens

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppActionButton
import com.example.ui.viewmodel.AuthUiState

@Composable
fun ForgotPasswordScreen(state: AuthUiState, onSubmit: (String) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var email by remember { mutableStateOf("") }
    val normalized = email.trim().lowercase()
    val emailValid = Patterns.EMAIL_ADDRESS.matcher(normalized).matches()
    val titleColor = Color(0xFF111827)
    val bodyColor = Color(0xFF475569)
    val errorColor = Color(0xFFDC2626)

    Column(
        Modifier.fillMaxSize().background(Color.White).imePadding().navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "뒤로가기", tint = titleColor) }
            Text("비밀번호 찾기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = titleColor)
        }
        if (state.passwordResetSent) {
            Spacer(Modifier.height(28.dp))
            Text("메일을 확인해주세요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = titleColor)
            Text("입력하신 이메일이 등록되어 있다면\n비밀번호 재설정 안내 메일을 보내드렸습니다.\n\n메일이 보이지 않는 경우 스팸함도 확인해주세요.", color = bodyColor)
            Spacer(Modifier.weight(1f))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppActionButton, contentColor = Color.White)) { Text("로그인으로 돌아가기") }
        } else {
            Text("가입한 이메일 주소를 입력해주세요.\n비밀번호 재설정 안내 메일을 보내드립니다.", color = bodyColor)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("이메일 주소") },
                trailingIcon = { Icon(Icons.Default.Email, null) },
                singleLine = true,
                enabled = !state.isLoading,
                isError = email.isNotEmpty() && !emailValid,
                supportingText = { if (email.isNotEmpty() && !emailValid) Text("올바른 이메일 주소를 입력해주세요.", color = errorColor) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
            state.errorMessage?.let { Text(it, color = errorColor) }
            Button(
                onClick = { onSubmit(normalized) },
                enabled = emailValid && !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppActionButton, contentColor = Color.White, disabledContainerColor = AppActionButton.copy(alpha = .55f))
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("비밀번호 재설정 메일 받기")
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("로그인으로 돌아가기") }
        }
    }
}

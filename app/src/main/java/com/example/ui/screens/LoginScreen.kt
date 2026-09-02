package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.BuildConfig
import com.example.ui.theme.FreshError
import com.example.ui.theme.FreshIndigo
import com.example.ui.theme.FreshOutline
import com.example.ui.theme.FreshTextMuted
import com.example.ui.theme.FreshTextPrimary
import com.example.ui.theme.FreshTextVariant
import com.example.ui.theme.AppActionButton
import com.example.ui.viewmodel.AuthUiState
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onGoogleLogin: (String) -> Unit,
    onGoogleLoginFailed: () -> Unit,
    onInputChanged: () -> Unit,
    onOpenSignup: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val readableTextColor = FreshTextPrimary
    val readableLabelColor = FreshTextVariant
    val readablePlaceholderColor = FreshTextMuted
    val readableDisabledColor = FreshTextMuted.copy(alpha = 0.7f)
    val readableErrorColor = FreshError
    val accentColor = FreshIndigo
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = readableTextColor,
        unfocusedTextColor = readableTextColor,
        disabledTextColor = readableDisabledColor,
        cursorColor = accentColor,
        errorCursorColor = readableErrorColor,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        focusedBorderColor = accentColor,
        unfocusedBorderColor = FreshOutline,
        disabledBorderColor = FreshOutline.copy(alpha = 0.75f),
        errorBorderColor = readableErrorColor,
        focusedLabelColor = readableLabelColor,
        unfocusedLabelColor = readableLabelColor,
        disabledLabelColor = readableDisabledColor,
        errorLabelColor = readableErrorColor,
        focusedPlaceholderColor = readablePlaceholderColor,
        unfocusedPlaceholderColor = readablePlaceholderColor,
        disabledPlaceholderColor = readableDisabledColor,
        focusedTrailingIconColor = readableLabelColor,
        unfocusedTrailingIconColor = readableLabelColor,
        disabledTrailingIconColor = readableDisabledColor,
        errorTrailingIconColor = readableErrorColor
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.img_uchiso_app_icon_1785309286513),
            contentDescription = "우치소 로고",
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "우치소",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = readableTextColor
        )
        Text(
            text = "회원 계정으로 로그인해주세요",
            style = MaterialTheme.typography.bodyMedium,
            color = readableLabelColor
        )
        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                onInputChanged()
            },
            label = { Text("이메일") },
            placeholder = { Text("user@example.com") },
            singleLine = true,
            enabled = !state.isLoading,
            isError = state.emailError != null,
            supportingText = state.emailError?.let { message ->
                { Text(message, color = readableErrorColor) }
            },
            colors = fieldColors,
            trailingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                onInputChanged()
            },
            label = { Text("비밀번호") },
            placeholder = { Text("비밀번호를 입력해주세요") },
            singleLine = true,
            enabled = !state.isLoading,
            isError = state.passwordError != null,
            supportingText = state.passwordError?.let { message ->
                { Text(message, color = readableErrorColor) }
            },
            colors = fieldColors,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("비밀번호 찾기", color = accentColor)
        }

        state.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = readableErrorColor
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { onLogin(email, password) },
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppActionButton,
                contentColor = Color.White,
                disabledContainerColor = AppActionButton.copy(alpha = 0.55f),
                disabledContentColor = Color.White.copy(alpha = 0.85f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("로그인", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.HorizontalDivider(Modifier.weight(1f), color = FreshOutline)
            Text("또는", modifier = Modifier.padding(horizontal = 12.dp), color = readablePlaceholderColor)
            androidx.compose.material3.HorizontalDivider(Modifier.weight(1f), color = FreshOutline)
        }
        Spacer(modifier = Modifier.height(14.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = {
                scope.launch {
                    try {
                        val option = GetGoogleIdOption.Builder()
                            .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                            .setFilterByAuthorizedAccounts(false)
                            .setAutoSelectEnabled(false)
                            .build()
                        val result = CredentialManager.create(context).getCredential(
                            context,
                            GetCredentialRequest.Builder().addCredentialOption(option).build()
                        )
                        val credential = result.credential
                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            onGoogleLogin(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                        } else onGoogleLoginFailed()
                    } catch (_: Exception) { onGoogleLoginFailed() }
                }
            },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Image(painterResource(R.drawable.ic_google_g), contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("Google 계정으로 로그인", color = readableTextColor)
        }

        TextButton(onClick = onOpenSignup) {
            Text(
                text = "아직 회원이 아니신가요? 회원가입",
                color = accentColor
            )
        }

    }
}

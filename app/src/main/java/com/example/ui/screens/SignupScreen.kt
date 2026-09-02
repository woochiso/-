package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.BuildConfig
import com.example.R
import com.example.data.remote.dto.RegistrationRequest
import com.example.ui.theme.AppActionButton
import com.example.ui.viewmodel.AuthUiState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material.icons.filled.ChevronRight
import java.util.Calendar

@Composable
fun SignupScreen(state: AuthUiState, onSubmit: (RegistrationRequest) -> Unit, onGoogleSignup: (String) -> Unit, onGoogleSignupFailed: () -> Unit, onEmailChanged: (String) -> Unit, onCheckEmail: (String) -> Unit, onNicknameChanged: (String) -> Unit, onCheckNickname: (String) -> Unit, onBack: () -> Unit) {
    val pageBackground = Color.White
    val titleColor = Color(0xFF111827)
    val bodyColor = Color(0xFF1F2937)
    val secondaryColor = Color(0xFF64748B)
    val placeholderColor = Color(0xFF7A8494)
    val iconColor = Color(0xFF475569)
    val borderColor = Color(0xFFCBD5E1)
    val linkColor = Color(0xFF4A90E2)
    val errorColor = Color(0xFFDC2626)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }; var nickname by remember { mutableStateOf("") }; var year by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }; var occupation by remember { mutableStateOf("") }; var occupationOther by remember { mutableStateOf("") }
    var marital by remember { mutableStateOf("") }; var children by remember { mutableStateOf("") }; var purpose by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(false) }; var privacy by remember { mutableStateOf(false) }; var localError by remember { mutableStateOf<String?>(null) }
    var legalDocument by remember { mutableStateOf<Pair<String,String>?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }; var confirmVisible by remember { mutableStateOf(false) }
    val normalizedEmail = email.trim().lowercase()
    val emailFormatValid = android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()
    val allowedSpecials = "!@#$%^&*()_-+={}[]:;,.?/~"
    val passwordLengthValid = password.length in 8..15
    val passwordLetterValid = password.any { it in 'A'..'Z' || it in 'a'..'z' }
    val passwordNumberValid = password.any { it in '0'..'9' }
    val passwordSpecialValid = password.any { it in allowedSpecials }
    val passwordNoWhitespace = password.none(Char::isWhitespace)
    val passwordAllowedCharacters = password.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it in allowedSpecials }
    val passwordValid = passwordLengthValid && passwordLetterValid && passwordNumberValid && passwordSpecialValid && passwordNoWhitespace && passwordAllowedCharacters
    val emailCheckMatchesInput = state.emailCheckEmail == normalizedEmail
    val emailAvailable = emailCheckMatchesInput && state.isEmailAvailable == true
    val passwordsMatch = confirm.isNotEmpty() && password == confirm
    val canContinue = emailFormatValid && emailAvailable && passwordValid && passwordsMatch && !state.isCheckingEmail
    val phoneDigits = phone.filter(Char::isDigit)
    val phoneCharactersValid = phone.all { it.isDigit() || it == '-' }
    val phoneValid = phoneCharactersValid && Regex("^01[016789][0-9]{7,8}$").matches(phoneDigits)
    val trimmedNickname = nickname.trim()
    val nicknameLengthValid = trimmedNickname.length in 2..20
    val nicknameCharactersValid = trimmedNickname.matches(Regex("^[가-힣A-Za-z0-9_ ]+$"))
    val nicknameFormatValid = nicknameLengthValid && nicknameCharactersValid
    val nicknameCheckMatchesInput = state.nicknameCheckNickname == trimmedNickname
    val nicknameAvailable = nicknameCheckMatchesInput && state.isNicknameAvailable == true
    val birthYear = year.toIntOrNull()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val birthYearValid = year.length == 4 && birthYear != null && birthYear in 1900..currentYear
    val dropdownsValid = gender in setOf("MALE", "FEMALE", "NO_ANSWER") &&
        occupation in setOf("학생", "회사원", "공무원", "전문직", "자영업", "프리랜서", "주부", "무직", "기타") &&
        (occupation != "기타" || occupationOther.trim().length in 1..100) &&
        marital in setOf("SINGLE", "MARRIED", "DIVORCED", "BEREAVED", "OTHER") &&
        (children.toIntOrNull()?.let { it in 0..5 } == true) &&
        purpose in setOf("SELF_UNDERSTANDING", "STRESS_MANAGEMENT", "DEPRESSION_MANAGEMENT", "COUNSELING", "HABIT_BUILDING", "OTHER")
    val additionalInfoValid = phoneValid && nicknameFormatValid && nicknameAvailable && birthYearValid && dropdownsValid && terms && privacy && !state.isCheckingNickname

    LaunchedEffect(email) {
        onEmailChanged(email)
        if (emailFormatValid) {
            delay(600)
            if (normalizedEmail == email.trim().lowercase()) onCheckEmail(normalizedEmail)
        }
    }
    LaunchedEffect(nickname) {
        onNicknameChanged(nickname)
        if (nicknameFormatValid) {
            delay(600)
            if (trimmedNickname == nickname.trim()) onCheckNickname(trimmedNickname)
        }
    }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = bodyColor, unfocusedTextColor = bodyColor,
        disabledTextColor = secondaryColor, cursorColor = linkColor,
        focusedLabelColor = titleColor, unfocusedLabelColor = Color(0xFF374151),
        focusedPlaceholderColor = placeholderColor, unfocusedPlaceholderColor = placeholderColor,
        focusedBorderColor = linkColor, unfocusedBorderColor = borderColor,
        focusedContainerColor = pageBackground, unfocusedContainerColor = pageBackground,
        focusedTrailingIconColor = iconColor, unfocusedTrailingIconColor = iconColor,
        focusedSupportingTextColor = secondaryColor, unfocusedSupportingTextColor = secondaryColor
    )
    if (legalDocument != null) {
        LegalDocumentScreen(legalDocument!!.first, legalDocument!!.second) { legalDocument = null }
        return
    }
    BackHandler { if (step > 0) step-- else onBack() }
    CompositionLocalProvider(LocalContentColor provides bodyColor) {
    Column(Modifier.fillMaxSize().background(pageBackground).verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row { IconButton(onClick = { if (step > 0) step-- else onBack() }) { Icon(Icons.Default.ArrowBack, "뒤로가기", tint=titleColor) }; Text("회원가입", color=titleColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) }
        when (step) {
            0 -> {
                Text(buildAnnotatedString { append("회원 정보를 "); withStyle(SpanStyle(color = linkColor)) { append("입력해주세요") } }, color=titleColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("우치소 계정을 생성합니다.", color = secondaryColor)
                OutlinedTextField(email, { email = it; localError = null }, label = { Text("이메일") }, placeholder = { Text("이메일을 입력해주세요") }, colors = fieldColors, trailingIcon = { Icon(Icons.Default.Email, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, isError = email.isNotEmpty() && !emailFormatValid, supportingText = {
                    when {
                        email.isNotEmpty() && !emailFormatValid -> Text("올바른 이메일 주소를 입력해주세요.", color=errorColor)
                        emailCheckMatchesInput && state.emailCheckMessage != null -> Text(state.emailCheckMessage, color = when { state.isCheckingEmail -> secondaryColor; state.isEmailAvailable == true -> Color(0xFF15803D); else -> errorColor })
                    }
                }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it.take(64); localError = null }, label = { Text("비밀번호") }, placeholder = { Text("비밀번호를 입력해주세요") }, colors = fieldColors, visualTransformation = if(passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton({passwordVisible=!passwordVisible}) { Icon(if(passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "비밀번호 보기") } }, supportingText = {
                    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                        PasswordRule("8~15자", passwordLengthValid)
                        PasswordRule("영문 포함", passwordLetterValid)
                        PasswordRule("숫자 포함", passwordNumberValid)
                        PasswordRule("특수문자 포함", passwordSpecialValid)
                        PasswordRule("공백 없음", passwordNoWhitespace && passwordAllowedCharacters)
                    }
                }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(confirm, { confirm = it.take(64); localError = null }, label = { Text("비밀번호 확인") }, placeholder = { Text("비밀번호를 다시 입력해주세요") }, colors = fieldColors, visualTransformation = if(confirmVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton({confirmVisible=!confirmVisible}) { Icon(if(confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "비밀번호 확인 보기") } }, supportingText = {
                    if (confirm.isNotEmpty()) Text(if (passwordsMatch) "비밀번호가 일치합니다." else "비밀번호가 일치하지 않습니다.", color=if(passwordsMatch) Color(0xFF15803D) else errorColor)
                }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { if (canContinue) { localError = null; step = 1 } }, enabled=canContinue, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppActionButton, contentColor=Color.White, disabledContainerColor=AppActionButton.copy(alpha=.55f), disabledContentColor=Color.White.copy(alpha=.85f))) { Text("다음", color=Color.White) }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { HorizontalDivider(Modifier.weight(1f),color=borderColor); Text("또는 소셜 계정으로 가입", Modifier.padding(horizontal=12.dp),color=Color(0xFF374151)); HorizontalDivider(Modifier.weight(1f),color=borderColor) }
                OutlinedButton(onClick = { scope.launch { try { val option=GetGoogleIdOption.Builder().setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID).setFilterByAuthorizedAccounts(false).setAutoSelectEnabled(false).build(); val result=CredentialManager.create(context).getCredential(context,GetCredentialRequest.Builder().addCredentialOption(option).build()); val credential=result.credential; if(credential is CustomCredential && credential.type==GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) onGoogleSignup(GoogleIdTokenCredential.createFrom(credential.data).idToken) else onGoogleSignupFailed() } catch (_:Exception) { onGoogleSignupFailed() } } }, enabled=!state.isLoading, border=BorderStroke(1.dp,borderColor), colors=ButtonDefaults.outlinedButtonColors(containerColor=pageBackground,contentColor=titleColor), modifier=Modifier.fillMaxWidth().height(52.dp)) { androidx.compose.foundation.Image(painterResource(R.drawable.ic_google_g),null,Modifier.size(24.dp)); Spacer(Modifier.width(10.dp)); Text("Google 계정으로 가입", color=titleColor) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.Center, verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) { Text("이미 계정이 있으신가요?",color=Color(0xFF374151)); TextButton(onClick=onBack) { Text("로그인",color=linkColor) } }
            }
            1 -> {
                Text("이용약관 및 개인정보 처리방침 동의", color=titleColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("계속 진행하려면 약관에 동의해주세요.", color = secondaryColor)
                val checkboxColors=CheckboxDefaults.colors(checkedColor=AppActionButton,uncheckedColor=borderColor,checkmarkColor=Color.White)
                Row { Checkbox(terms && privacy, { terms = it; privacy = it },colors=checkboxColors); Text("모두 동의합니다.",color=bodyColor, modifier = Modifier.padding(top = 12.dp)) }
                Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) { Checkbox(terms, { terms = it },colors=checkboxColors); Text("[필수] 우치소 이용약관 동의",color=bodyColor,modifier=Modifier.weight(1f)); IconButton(onClick={legalDocument="이용약관" to "https://woochiso.com/terms.php"}) { Icon(Icons.Default.ChevronRight,"이용약관 보기",tint=Color(0xFF475569)) } }
                Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) { Checkbox(privacy, { privacy = it },colors=checkboxColors); Text("[필수] 개인정보 수집 및 이용 동의",color=bodyColor,modifier=Modifier.weight(1f)); IconButton(onClick={legalDocument="개인정보 수집 및 이용" to "https://woochiso.com/privacy-consent.php"}) { Icon(Icons.Default.ChevronRight,"개인정보 수집 및 이용 보기",tint=Color(0xFF475569)) } }
                Button(onClick = { if (terms && privacy) { localError = null; step = 2 } else localError = "필수 약관에 모두 동의해주세요." }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppActionButton,contentColor=Color.White)) { Text("다음",color=Color.White) }
            }
            else -> {
                Text("추가 정보를 입력해주세요", color=titleColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(phone, { phone = it.take(20); localError=null }, label = { Text("휴대폰 번호") }, colors = fieldColors, isError=phone.isNotEmpty()&&!phoneValid, supportingText={ if(phone.isNotEmpty()&&!phoneValid) Text("올바른 휴대폰 번호를 입력해주세요.",color=errorColor) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(nickname, { nickname = it.take(30); localError=null }, label = { Text("닉네임") }, colors = fieldColors, isError=nickname.isNotEmpty()&&!nicknameFormatValid, supportingText = {
                    when {
                        nickname.isNotEmpty() && !nicknameLengthValid -> Text("닉네임은 2~20자로 입력해주세요.",color=errorColor)
                        nickname.isNotEmpty() && !nicknameCharactersValid -> Text("닉네임은 한글, 영문, 숫자, 공백, 밑줄만 사용할 수 있습니다.",color=errorColor)
                        nicknameCheckMatchesInput && state.nicknameCheckMessage != null -> Text(state.nicknameCheckMessage,color=when { state.isCheckingNickname -> secondaryColor; state.isNicknameAvailable==true -> Color(0xFF15803D); else -> errorColor })
                        else -> Text("한글, 영문, 숫자, 공백, 밑줄 2~20자",color=secondaryColor)
                    }
                }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(year, { year = it.take(4); localError=null }, label = { Text("출생연도") }, colors = fieldColors, isError=year.isNotEmpty()&&!birthYearValid, supportingText={
                    if(year.isNotEmpty()&&!birthYearValid) Text(if(birthYear!=null&&birthYear>currentYear) "현재 연도보다 이후의 연도는 입력할 수 없습니다." else "올바른 출생연도를 입력해주세요.",color=errorColor)
                }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                RegisterDropdown("성별", gender, listOf("MALE" to "남성", "FEMALE" to "여성", "NO_ANSWER" to "응답하지 않음")) { gender = it }
                RegisterDropdown("직업", occupation, listOf("학생","회사원","공무원","전문직","자영업","프리랜서","주부","무직","기타").map { it to it }) { occupation = it }
                if (occupation == "기타") OutlinedTextField(occupationOther, { occupationOther = it.take(100) }, label = { Text("기타 직업") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                RegisterDropdown("결혼 여부", marital, listOf("SINGLE" to "미혼", "MARRIED" to "기혼", "DIVORCED" to "이혼", "BEREAVED" to "사별", "OTHER" to "기타")) { marital = it }
                RegisterDropdown("자녀 수", children, (0..5).map { it.toString() to if (it == 5) "5명 이상" else "${it}명" }) { children = it }
                RegisterDropdown("감정 기록 목적", purpose, listOf("SELF_UNDERSTANDING" to "나를 이해하기 위해", "STRESS_MANAGEMENT" to "스트레스 관리", "DEPRESSION_MANAGEMENT" to "우울감 관리", "COUNSELING" to "상담을 위해", "HABIT_BUILDING" to "습관 만들기", "OTHER" to "기타")) { purpose = it }
                state.errorMessage?.let { Text(it,color=errorColor,style=MaterialTheme.typography.bodyMedium) }
                Button(onClick = {
                    if (additionalInfoValid && birthYear != null) onSubmit(RegistrationRequest(normalizedEmail,password,confirm,phone,trimmedNickname,birthYear,gender,occupation,occupationOther.trim().ifBlank { null },marital,children.toInt(),purpose,terms,privacy))
                }, enabled = additionalInfoValid && !state.isLoading, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppActionButton,contentColor=Color.White,disabledContainerColor=AppActionButton.copy(alpha=.55f),disabledContentColor=Color.White.copy(alpha=.85f))) { if (state.isLoading) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,color=Color.White); Spacer(Modifier.width(8.dp)); Text("가입 처리 중...",color=Color.White) } else Text("가입 완료",color=Color.White) }
            }
        }
        (localError ?: state.errorMessage?.takeUnless { step == 2 })?.let { Text(it, color = errorColor) }
    }
    }
}

@Composable
private fun PasswordRule(label: String, satisfied: Boolean) {
    Text(
        text = if (satisfied) "✓ $label" else "○ $label",
        color = if (satisfied) Color(0xFF15803D) else Color(0xFF64748B),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun LegalDocumentScreen(title:String,url:String,onClose:()->Unit) {
    BackHandler(onBack=onClose)
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick=onClose) { Icon(Icons.Default.ArrowBack,"뒤로가기",tint=Color(0xFF111827)) }
            Text(title,color=Color(0xFF111827),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        }
        AndroidView(factory={ context -> WebView(context).apply { setBackgroundColor(android.graphics.Color.WHITE); settings.javaScriptEnabled=false; settings.domStorageEnabled=false; webViewClient=WebViewClient(); loadUrl(url) } },modifier=Modifier.weight(1f).fillMaxWidth())
        Button(onClick=onClose,modifier=Modifier.fillMaxWidth().padding(16.dp),colors=ButtonDefaults.buttonColors(containerColor=AppActionButton,contentColor=Color.White)) { Text("확인",color=Color.White) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun RegisterDropdown(label: String, value: String, options: List<Pair<String,String>>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }; val shown = options.firstOrNull { it.first == value }?.second.orEmpty()
    val colors = OutlinedTextFieldDefaults.colors(focusedTextColor=Color(0xFF1F2937),unfocusedTextColor=Color(0xFF1F2937),focusedLabelColor=Color(0xFF111827),unfocusedLabelColor=Color(0xFF374151),focusedBorderColor=Color(0xFF4A90E2),unfocusedBorderColor=Color(0xFFCBD5E1),focusedContainerColor=Color.White,unfocusedContainerColor=Color.White,focusedTrailingIconColor=Color(0xFF475569),unfocusedTrailingIconColor=Color(0xFF475569))
    ExposedDropdownMenuBox(open, { open = !open }) { OutlinedTextField(shown, {}, readOnly = true, label = { Text(label) }, colors=colors, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()); ExposedDropdownMenu(open, { open = false }) { options.forEach { (key,text) -> DropdownMenuItem({ Text(text) }, { onSelect(key); open = false }) } } }
}

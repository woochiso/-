package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.data.remote.dto.GoogleSignupRequest
import com.example.ui.theme.AppActionButton
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.GoogleSignupState
import java.util.Calendar

@Composable
fun GoogleSignupScreen(state: AuthUiState, signup: GoogleSignupState, onSubmit: (GoogleSignupRequest) -> Unit, onCancel: () -> Unit) {
    var phone by remember { mutableStateOf("") }; var nickname by remember { mutableStateOf(signup.profile.name.orEmpty().take(20)) }; var birthYear by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }; var occupation by remember { mutableStateOf("") }; var occupationOther by remember { mutableStateOf("") }; var marital by remember { mutableStateOf("") }; var children by remember { mutableStateOf("") }; var purpose by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(false) }; var privacy by remember { mutableStateOf(false) }
    val phoneDigits = phone.filter(Char::isDigit)
    val phoneValid = phone.all { it.isDigit() || it == '-' } && Regex("^01[016789][0-9]{7,8}$").matches(phoneDigits)
    val cleanNickname = nickname.trim()
    val nicknameValid = cleanNickname.length in 2..20 && cleanNickname.matches(Regex("^[가-힣A-Za-z0-9_ ]+$"))
    val year = birthYear.toIntOrNull()
    val valid = phoneValid && nicknameValid && birthYear.length == 4 && year != null && year in 1900..Calendar.getInstance().get(Calendar.YEAR) &&
        gender in setOf("MALE", "FEMALE", "NO_ANSWER") &&
        occupation in setOf("학생", "회사원", "공무원", "전문직", "자영업", "프리랜서", "주부", "무직", "기타") &&
        (occupation != "기타" || occupationOther.trim().length in 1..100) &&
        marital in setOf("SINGLE", "MARRIED", "DIVORCED", "BEREAVED", "OTHER") &&
        (children.toIntOrNull()?.let { it in 0..5 } == true) &&
        purpose in setOf("SELF_UNDERSTANDING", "STRESS_MANAGEMENT", "DEPRESSION_MANAGEMENT", "COUNSELING", "HABIT_BUILDING", "OTHER") && terms && privacy
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("가입 정보를 완성해주세요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Google에서 확인한 이메일에 우치소 회원정보를 연결합니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(signup.profile.email, {}, readOnly = true, label = { Text("Google 이메일") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it.take(13) }, label = { Text("휴대폰 번호") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(nickname, { nickname = it.take(20) }, label = { Text("닉네임") }, supportingText = { Text("한글, 영문, 숫자, 공백, 밑줄 2~20자") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(birthYear, { birthYear = it.filter(Char::isDigit).take(4) }, label = { Text("출생연도") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        SignupDropdown("성별", gender, listOf("MALE" to "남성", "FEMALE" to "여성", "NO_ANSWER" to "응답하지 않음")) { gender = it }
        SignupDropdown("직업", occupation, listOf("학생","회사원","공무원","전문직","자영업","프리랜서","주부","무직","기타").map { it to it }) { occupation = it }
        if (occupation == "기타") OutlinedTextField(occupationOther, { occupationOther = it.take(100) }, label = { Text("기타 직업 직접 입력") }, modifier = Modifier.fillMaxWidth())
        SignupDropdown("결혼 여부", marital, listOf("SINGLE" to "미혼","MARRIED" to "기혼","DIVORCED" to "이혼","BEREAVED" to "사별","OTHER" to "기타")) { marital = it }
        SignupDropdown("자녀 수", children, (0..5).map { it.toString() to if(it==5) "5명 이상" else "${it}명" }) { children = it }
        Text("감정을 기록하는 목적", fontWeight = FontWeight.Bold)
        listOf("SELF_UNDERSTANDING" to "나를 이해하기 위해","STRESS_MANAGEMENT" to "스트레스 관리","DEPRESSION_MANAGEMENT" to "우울감 관리","COUNSELING" to "상담을 위해","HABIT_BUILDING" to "습관 만들기","OTHER" to "기타").forEach { (value,label) -> FilterChip(selected=purpose==value,onClick={purpose=value},label={Text(label)}) }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Checkbox(terms,{terms=it}); Text("[필수] 이용약관 동의 (${signup.consentVersions?.terms.orEmpty()})") }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Checkbox(privacy,{privacy=it}); Text("[필수] 개인정보 수집·이용 동의 (${signup.consentVersions?.privacyCollection.orEmpty()})") }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick={onSubmit(GoogleSignupRequest(signup.idToken,phone,nickname.trim(),birthYear.toInt(),gender,occupation,occupationOther.trim().ifBlank { null },marital,children.toInt(),purpose,terms,privacy))},enabled=valid&&!state.isLoading,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=AppActionButton)){if(state.isLoading)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else Text("Google 계정으로 가입하기")}
        TextButton(onClick=onCancel,modifier=Modifier.fillMaxWidth()){Text("로그인으로 돌아가기")}
        Spacer(Modifier.navigationBarsPadding())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SignupDropdown(label:String,value:String,options:List<Pair<String,String>>,onSelect:(String)->Unit){var open by remember{mutableStateOf(false)};val shown=options.firstOrNull{it.first==value}?.second.orEmpty();ExposedDropdownMenuBox(open,{open=!open}){OutlinedTextField(shown,{},readOnly=true,label={Text(label)},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(open)},modifier=Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth());ExposedDropdownMenu(open,{open=false}){options.forEach{(v,t)->DropdownMenuItem({Text(t)},{onSelect(v);open=false})}}}}

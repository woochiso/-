package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.ProfileUiState

private val MyInfoAccent = Color(0xFF74AFDD)
private val MyInfoLightBackground = Color(0xFFEAF2FB)

@Composable
fun MyInfoScreen(
    nickname: String?,
    email: String?,
    grade: String?,
    profileState: ProfileUiState,
    onLoadProfile: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenHelpVideos: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenVoiceGuide: () -> Unit,
    onLogout: () -> Unit
) {
    val displayName = nickname?.takeIf(String::isNotBlank) ?: "테스트 사용자"
    val displayEmail = email?.takeIf(String::isNotBlank) ?: "test@example.com"
    val displayGrade = grade?.takeIf(String::isNotBlank) ?: "FREE"
    LaunchedEffect(Unit) { onLoadProfile() }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            Text(
                text = "내정보",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "내 계정과 우치소 이용 정보를 확인하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { Spacer(modifier = Modifier.height(6.dp)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = MyInfoLightBackground, shape = CircleShape) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MyInfoAccent,
                            modifier = Modifier.padding(10.dp).size(36.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(displayEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(color = MyInfoLightBackground, shape = RoundedCornerShape(9.dp)) {
                        Text(
                            text = displayGrade,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MyInfoAccent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            Text("나의 이용 현황", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("서버에 저장된 현재 계정의 활동입니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (profileState.isLoading && profileState.usage == null) {
            item { Text("이용 현황을 불러오는 중...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else if (profileState.error != null && profileState.usage == null) {
            item { Text("이용 현황을 불러오지 못했습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        profileState.usage?.let { usage ->
            item { UsageRow("가입일", usage.joinedAt?.take(10)?.replace('-', '.') ?: "-", "감정 기록일 수", "${usage.emotionRecordDays}일") }
            item { UsageRow("총 감정 발생", "${usage.emotionTotalCount}회", "등록한 나의 사연", "${usage.storyCount}개") }
            item { UsageRow("AI 상담", "${usage.counselingCount}회", "회복 활동", "${usage.recoveryCount}회") }
        }
        item {
            Text("AI 음성 이용 현황", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("홈페이지와 Android의 Typecast 음성 생성 횟수를 합산합니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (profileState.isLoading && profileState.tts == null) {
            item { Text("AI 음성 이용 현황을 불러오는 중...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else if (profileState.error != null && profileState.tts == null) {
            item { Text("AI 음성 이용 현황을 불러오지 못했습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        profileState.tts?.let { tts ->
            item {
                val used = if (tts.limited) minOf(tts.used, tts.limit ?: tts.used) else tts.used
                UsageRow(
                    "오늘 사용", "${used}회",
                    if (tts.limited) "오늘 남은 횟수" else "이용 가능 상태",
                    if (tts.limited) "${tts.remaining ?: 0}회" else "제한 없음"
                )
            }
            item {
                Text(
                    if (tts.limited) "FREE 회원은 AI 음성을 하루 최대 30회 이용할 수 있습니다.\n매일 자정(한국시간)에 초기화됩니다."
                    else "현재 회원등급은 AI 음성 일일 횟수 제한이 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            TextButton(onClick = onOpenVoiceGuide) { Text("AI 음성 이용안내") }
        }
        item { MyInfoMenuCard("회원정보 수정", "닉네임과 회원정보를 관리합니다.", Icons.Default.Badge, onEditProfile) }
        item { MyInfoMenuCard("비밀번호 변경", "로그인 비밀번호를 변경합니다.", Icons.Default.LockReset, onChangePassword) }
        item { MyInfoMenuCard("우치소 소개 및 사용방법", "소개와 사용방법 영상을 확인합니다.", Icons.Default.OndemandVideo, onOpenHelpVideos) }
        item { MyInfoMenuCard("고객센터", "공지사항, FAQ 및 문의를 확인합니다.", Icons.Default.Help, onOpenSupport) }
        item { MyInfoMenuCard("앱 정보", "감정다이어리 앱 정보를 확인합니다.", Icons.Default.Info, onOpenAppInfo) }
        item { Spacer(modifier = Modifier.height(4.dp)) }
        item {
            MyInfoMenuCard(
                title = "로그아웃",
                description = "현재 계정에서 로그아웃합니다.",
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = onLogout,
                showArrow = false,
                containerColor = MyInfoLightBackground
            )
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun UsageRow(firstLabel: String, firstValue: String, secondLabel: String, secondValue: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        UsageItem(firstLabel, firstValue, Modifier.weight(1f))
        UsageItem(secondLabel, secondValue, Modifier.weight(1f))
    }
}

@Composable
private fun UsageItem(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MyInfoLightBackground),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun MyInfoMenuCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MyInfoAccent, modifier = Modifier.size(25.dp))
            Column(modifier = Modifier.padding(horizontal = 13.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

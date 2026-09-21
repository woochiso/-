package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import java.util.Calendar

private val AppInfoAccent = Color(0xFF74AFDD)
private val AppInfoNavy = Color(0xFF15233A)
private val AppInfoText = Color(0xFF475569)
private val AppInfoCard = Color(0xFFF3F8FC)
private val AppInfoBorder = Color(0xFFD8E6F2)
private const val PrivacyPolicyUrl = "https://woochiso.com/privacy.php"
private const val SupportEmail = "admin@woochiso.com"

@Composable
fun AppInfoScreen() {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(R.drawable.img_uchiso_app_icon_1785309286513),
                    contentDescription = "우치소 로고",
                    modifier = Modifier.size(82.dp),
                    contentScale = ContentScale.Crop
                )
                Text("우치소 WOOCHISO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppInfoNavy, modifier = Modifier.padding(top = 12.dp))
                Text(
                    "내 감정을 기록하고 이해하며,\nAI와 함께 마음을 돌아보는 감정관리 서비스",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppInfoText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 7.dp)
                )
            }
        }
        item { SectionTitle("주요 기능") }
        item { FeatureCard(Icons.Default.MenuBook, "감정다이어리", "하루의 감정과 마음의 변화를 기록합니다.") }
        item { FeatureCard(Icons.Default.AutoAwesome, "AI 감정분석", "기록된 감정과 활동 데이터를 바탕으로 감정의 변화를 살펴봅니다.") }
        item { FeatureCard(Icons.Default.ChatBubbleOutline, "AI 상담", "AI와 대화하며 자신의 생각과 감정을 정리해볼 수 있습니다.") }
        item { FeatureCard(Icons.Default.Spa, "AI 감정회복", "다양한 활동을 통해 감정을 환기하고 변화를 기록합니다.") }
        item { FeatureCard(Icons.Default.Psychology, "AI TRAINING", "다양한 AI 연습을 통해 표현력과 대화 능력을 연습할 수 있습니다.") }
        item {
            InfoCard {
                Text("AI TRAINING", fontWeight = FontWeight.Bold, color = AppInfoNavy)
                Spacer(Modifier.height(8.dp))
                Text(
                    "• AI를 웃겨라\n• AI 소개팅\n• AI와 감정표현 연습\n• AI 상식퀴즈\n• AI 토론연습\n• AI 보컬트레이닝\n• AI 재치와 센스",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppInfoText
                )
            }
        }
        item { SectionTitle("앱 정보") }
        item {
            InfoCard {
                InfoRow("앱 이름", "감정다이어리")
                InfoRow("서비스", "우치소 WOOCHISO")
                InfoRow("버전", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            }
        }
        item {
            InfoCard {
                Text("우치소의 AI 기능은 자기이해와 감정 기록을 돕기 위한 참고용 서비스입니다.", color = AppInfoNavy, fontWeight = FontWeight.SemiBold)
                Text("의료적 진단이나 전문적인 치료를 대체하지 않습니다.", color = AppInfoText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 7.dp))
            }
        }
        item { LinkRow("개인정보처리방침", null) { openUri(context, Uri.parse(PrivacyPolicyUrl), "개인정보처리방침을 열 수 없습니다.") } }
        item { SectionTitle("문의") }
        item { LinkRow(SupportEmail, Icons.Default.Email) { openEmail(context) } }
        item {
            Text(
                "© ${Calendar.getInstance().get(Calendar.YEAR)} WOOCHISO",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = AppInfoText
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppInfoNavy, modifier = Modifier.padding(top = 4.dp))

@Composable
private fun FeatureCard(icon: ImageVector, title: String, description: String) = InfoCard {
    Row(verticalAlignment = Alignment.Top) {
        Surface(color = Color.White, shape = MaterialTheme.shapes.medium) {
            Icon(icon, contentDescription = null, tint = AppInfoAccent, modifier = Modifier.padding(10.dp).size(24.dp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = AppInfoNavy)
            Text(description, style = MaterialTheme.typography.bodySmall, color = AppInfoText, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) = Surface(
    modifier = Modifier.fillMaxWidth(),
    color = AppInfoCard,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, AppInfoBorder)
) { Column(Modifier.padding(16.dp), content = content) }

@Composable
private fun InfoRow(label: String, value: String) = Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, color = AppInfoText, style = MaterialTheme.typography.bodyMedium)
    Text(value, color = AppInfoNavy, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun LinkRow(title: String, icon: ImageVector?, onClick: () -> Unit) = Surface(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    color = Color.White,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, AppInfoBorder)
) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, contentDescription = null, tint = AppInfoAccent, modifier = Modifier.size(22.dp).padding(end = 4.dp))
        Text(title, modifier = Modifier.weight(1f), color = AppInfoNavy, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppInfoAccent)
    }
}

private fun openUri(context: Context, uri: Uri, errorMessage: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun openEmail(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SupportEmail")))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "이메일 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}

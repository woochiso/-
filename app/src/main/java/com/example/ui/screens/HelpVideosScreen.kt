package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.dto.HelpVideo
import com.example.ui.theme.AppActionButton
import com.example.ui.viewmodel.HelpVideoViewModel

@Composable
fun HelpVideosScreen(viewModel: HelpVideoViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.load() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("우치소를 처음 이용하시나요?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("소개와 사용방법 영상을 확인해 보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.error?.let { message -> item { HelpVideoError(message, viewModel::load) } }
            if (!state.loading && state.error == null && state.videos.isEmpty()) {
                item { Text("등록된 사용방법 영상이 없습니다.", modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.videos, key = HelpVideo::id) { video ->
                HelpVideoCard(video) { openYoutube(context, video.youtubeUrl) }
            }
        }
        if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = AppActionButton)
    }
}

@Composable
private fun HelpVideoCard(video: HelpVideo, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = AppActionButton.copy(alpha = .13f), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.PlayCircle, null, tint = AppActionButton, modifier = Modifier.padding(12.dp).size(30.dp))
            }
            Column(Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(video.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (video.description.isNotBlank()) Text(video.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = onOpen, contentPadding = PaddingValues(0.dp)) {
                    Text("영상 보기", color = AppActionButton, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(5.dp)); Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = AppActionButton, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun HelpVideoError(message: String, retry: () -> Unit) = Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
        TextButton(onClick = retry) { Text("다시 시도") }
    }
}

private fun openYoutube(context: Context, url: String) {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.youtube"))
    } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}

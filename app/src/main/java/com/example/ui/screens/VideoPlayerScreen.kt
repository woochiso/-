package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.HelpVideoViewModel
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

enum class HelpVideoType(val apiValue: String) {
    INTRO("INTRO"), EMOTION_DIARY("EMOTION_DIARY"), AI_CARE("AI_CARE")
}

private data class FullscreenPlayer(val view: View, val exit: () -> Unit, val previousOrientation: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoType: HelpVideoType,
    title: String,
    viewModel: HelpVideoViewModel,
    onBack: () -> Unit
) {
    val activity = LocalContext.current as Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var playerReady by remember(videoType) { mutableStateOf(false) }
    var playerLoadFailed by remember(videoType) { mutableStateOf(false) }
    var retryGeneration by remember(videoType) { mutableIntStateOf(0) }
    var fullscreenPlayer by remember { mutableStateOf<FullscreenPlayer?>(null) }

    fun restoreSystemUi(previousOrientation: Int) {
        activity.requestedOrientation = previousOrientation
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    fun closeFullscreen() { fullscreenPlayer?.exit?.invoke() }

    LaunchedEffect(videoType) { viewModel.load() }

    DisposableEffect(Unit) {
        onDispose {
            fullscreenPlayer?.let {
                it.exit()
                restoreSystemUi(it.previousOrientation)
            }
        }
    }

    BackHandler { if (fullscreenPlayer != null) closeFullscreen() else onBack() }

    val selectedVideo = state.videos.firstOrNull { it.type == videoType.apiValue }
    val videoId = selectedVideo?.youtubeUrl?.let(::youtubeId)
    LaunchedEffect(selectedVideo?.youtubeUrl) {
        selectedVideo?.let { Log.d(VIDEO_LOG_TAG, "videoType=${videoType.apiValue}, apiSuccess=true, videoId=$videoId") }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = { if (fullscreenPlayer != null) closeFullscreen() else onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                state.loading && selectedVideo == null -> CircularProgressIndicator()
                state.error != null || playerLoadFailed -> VideoLoadError {
                    playerLoadFailed = false
                    playerReady = false
                    retryGeneration++
                    viewModel.load()
                }
                selectedVideo == null -> Text("영상 준비 중입니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                videoId == null -> InvalidVideoError()
                else -> key(videoId, retryGeneration) {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                YouTubePlayerView(context).apply {
                                    lifecycleOwner.lifecycle.addObserver(this)
                                    enableAutomaticInitialization = false
                                    addFullscreenListener(object : FullscreenListener {
                                        override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                                            val previous = activity.requestedOrientation
                                            fullscreenPlayer = FullscreenPlayer(fullscreenView, exitFullscreen, previous)
                                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                                                .hide(WindowInsetsCompat.Type.systemBars())
                                            Log.d(VIDEO_LOG_TAG, "fullscreenEntered videoType=${videoType.apiValue}")
                                        }

                                        override fun onExitFullscreen() {
                                            val previous = fullscreenPlayer?.previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                            fullscreenPlayer = null
                                            restoreSystemUi(previous)
                                            Log.d(VIDEO_LOG_TAG, "fullscreenExited videoType=${videoType.apiValue}")
                                        }
                                    })
                                    val listener = object : AbstractYouTubePlayerListener() {
                                        override fun onReady(youTubePlayer: YouTubePlayer) {
                                            playerReady = true
                                            playerLoadFailed = false
                                            youTubePlayer.cueVideo(videoId, 0f)
                                            Log.d(VIDEO_LOG_TAG, "playerReady videoType=${videoType.apiValue}, videoId=$videoId")
                                        }

                                        override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                                            Log.e(VIDEO_LOG_TAG, "playerError videoType=${videoType.apiValue}, error=$error")
                                            playerLoadFailed = true
                                        }
                                    }
                                    val options = IFramePlayerOptions.Builder(context)
                                        .controls(1).fullscreen(1).rel(0).ccLoadPolicy(1).build()
                                    initialize(listener, true, options, videoId)
                                }
                            },
                            onRelease = { view ->
                                lifecycleOwner.lifecycle.removeObserver(view)
                                view.release()
                            }
                        )
                        if (!playerReady) CircularProgressIndicator(color = Color.White)
                    }
                }
                }
            }
        }

        fullscreenPlayer?.let { fullscreen ->
            AndroidView(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                factory = {
                    (fullscreen.view.parent as? ViewGroup)?.removeView(fullscreen.view)
                    fullscreen.view
                }
            )
        }
    }
}

@Composable
private fun InvalidVideoError() {
    Text("영상 정보를 확인할 수 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun VideoLoadError(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("영상을 불러올 수 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("다시 시도") }
    }
}

internal fun youtubeId(url: String): String? {
    val uri = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return null
    val host = uri.host?.lowercase().orEmpty().removePrefix("www.")
    val candidate = when {
        host == "youtu.be" -> uri.pathSegments.firstOrNull()
        host == "youtube.com" || host == "m.youtube.com" -> when {
            uri.pathSegments.firstOrNull() == "shorts" -> uri.pathSegments.getOrNull(1)
            uri.pathSegments.firstOrNull() == "embed" -> uri.pathSegments.getOrNull(1)
            else -> uri.getQueryParameter("v")
        }
        else -> null
    }
    return candidate?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
}

private const val VIDEO_LOG_TAG = "WOOCHISO_VIDEO"

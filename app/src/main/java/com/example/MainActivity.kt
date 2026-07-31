package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.EmotionDiaryScreen
import com.example.ui.screens.FavoriteEmotionsScreen
import com.example.ui.screens.HumanEmotionsScreen
import com.example.ui.screens.InnerStoriesScreen
import com.example.ui.screens.OnboardingNicknameScreen
import com.example.ui.theme.EmotionDiaryTheme
import com.example.ui.viewmodel.EmotionViewModel

enum class NavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HUMAN_EMOTIONS("인간의 감정", Icons.Default.SelfImprovement, Icons.Outlined.SelfImprovement),
    FAVORITE_EMOTIONS("즐찾감정", Icons.Default.Star, Icons.Outlined.StarBorder),
    EMOTION_DIARY("감정다이어리", Icons.Default.PieChart, Icons.Outlined.PieChartOutline),
    INNER_STORIES("나의 사연", Icons.Default.MenuBook, Icons.Outlined.MenuBook)
}

class MainActivity : ComponentActivity() {

    private val viewModel: EmotionViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EmotionDiaryTheme {
                val context = LocalContext.current
                var selectedTabIndex by remember { mutableIntStateOf(0) }

                val favorites by viewModel.favorites.collectAsStateWithLifecycle()
                val diaryEntries by viewModel.diaryEntries.collectAsStateWithLifecycle()
                val filteredDiaryEntries by viewModel.filteredDiaryEntries.collectAsStateWithLifecycle()
                val innerStories by viewModel.innerStories.collectAsStateWithLifecycle()
                val humanEmotionsList by viewModel.humanEmotionsList.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
                val emotionCategoryStats by viewModel.emotionCategoryStats.collectAsStateWithLifecycle()
                val selectedDateRangeText by viewModel.selectedDateRangeText.collectAsStateWithLifecycle()
                val selectedChartRange by viewModel.selectedChartRange.collectAsStateWithLifecycle()

                val userNickname by viewModel.userNickname.collectAsStateWithLifecycle()
                var showEditNicknameDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.toastEvent.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                if (userNickname.isNullOrBlank()) {
                    OnboardingNicknameScreen(
                        onNicknameSaved = { newNickname ->
                            viewModel.saveUserNickname(newNickname)
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_uchiso_app_icon_1785309286513),
                                            contentDescription = "App Icon",
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "감정 다이어리(우치소)",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${userNickname}님의 감정 공간 • 희노애락애오욕",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    Surface(
                                        onClick = { showEditNicknameDialog = true },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Face,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = userNickname ?: "",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "별명 수정",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = NavigationBarDefaults.windowInsets
                        ) {
                            NavTab.entries.forEachIndexed { index, tab ->
                                val isSelected = selectedTabIndex == index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedTabIndex = index },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = selectedTabIndex, label = "tab_crossfade") { tabIndex ->
                            when (NavTab.entries[tabIndex]) {
                                NavTab.HUMAN_EMOTIONS -> {
                                    HumanEmotionsScreen(
                                        viewModel = viewModel,
                                        emotionsList = humanEmotionsList,
                                        selectedCategory = selectedCategoryFilter
                                    )
                                }
                                NavTab.FAVORITE_EMOTIONS -> {
                                    FavoriteEmotionsScreen(
                                        viewModel = viewModel,
                                        favorites = favorites,
                                        innerStories = innerStories
                                    )
                                }
                                NavTab.EMOTION_DIARY -> {
                                    EmotionDiaryScreen(
                                        viewModel = viewModel,
                                        diaryEntries = filteredDiaryEntries,
                                        emotionCategoryStats = emotionCategoryStats,
                                        selectedDateRangeText = selectedDateRangeText,
                                        selectedRange = selectedChartRange,
                                        innerStories = innerStories
                                    )
                                }
                                NavTab.INNER_STORIES -> {
                                    InnerStoriesScreen(
                                        viewModel = viewModel,
                                        stories = innerStories
                                    )
                                }
                            }
                        }
                    }
                }

                if (showEditNicknameDialog && !userNickname.isNullOrBlank()) {
                    var editInput by remember { mutableStateOf(userNickname ?: "") }
                    AlertDialog(
                        onDismissRequest = { showEditNicknameDialog = false },
                        title = {
                            Text(
                                text = "별명 수정",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    text = "새로 사용하실 별명을 입력해 주세요.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = editInput,
                                    onValueChange = { editInput = it },
                                    label = { Text("별명 / 닉네임") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (editInput.trim().isNotBlank()) {
                                        viewModel.updateNickname(editInput)
                                        showEditNicknameDialog = false
                                    }
                                }
                            ) {
                                Text("저장")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditNicknameDialog = false }) {
                                Text("취소")
                            }
                        }
                    )
                }
            }
            }
        }
    }
}


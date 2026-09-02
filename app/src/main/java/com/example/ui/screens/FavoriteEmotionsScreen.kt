package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FavoriteEmotionEntity
import com.example.data.local.entity.InnerStoryEntity
import com.example.data.remote.dto.FavoriteEmotionServerDto
import com.example.data.model.EmotionCategory
import com.example.ui.viewmodel.EmotionViewModel
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.parseServerColor
import com.example.ui.theme.EmotionColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteEmotionsScreen(
    viewModel: EmotionViewModel,
    favorites: List<FavoriteEmotionEntity>,
    innerStories: List<InnerStoryEntity>,
    onOpenInnerStories: () -> Unit = {},
    showPageTitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFavForStory by remember { mutableStateOf<FavoriteEmotionServerDto?>(null) }
    var favoritePendingRemoval by remember { mutableStateOf<FavoriteEmotionServerDto?>(null) }
    var correctionPending by remember { mutableStateOf<FavoriteEmotionServerDto?>(null) }
    var correctionValue by remember { mutableStateOf(0) }
    val serverState by viewModel.favoriteServerState.collectAsStateWithLifecycle()
    val storyState by viewModel.emotionStoryState.collectAsStateWithLifecycle()
    val storyConnections by viewModel.emotionStoryConnections.collectAsStateWithLifecycle()
    val displayedFavorites = serverState.favorites

    LaunchedEffect(Unit) { viewModel.loadServerFavorites() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Screen Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. 즐찾감정 (자주 느끼는 감정)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "오늘 새롭게 느낀 횟수를 선택하고 추가 기록하면 서버의 오늘 누적에 안전하게 더해집니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (serverState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (serverState.error != null && displayedFavorites.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(serverState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.loadServerFavorites() }) { Text("다시 시도") }
                    }
                }
            } else if (displayedFavorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "저장된 즐찾 감정이 없습니다.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "'인간의 감정' 탭에서 별(⭐) 아이콘을 누르거나\n아래 우측 하단 (+) 버튼으로 감정을 추가해보세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(displayedFavorites, key = { it.emotionId }) { fav ->
                        val serverColor = EmotionColors.forCategoryCode(
                            fav.categoryCode,
                            parseServerColor(fav.colorCode)
                        )
                        val connectedStory = storyConnections[fav.emotionId]

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, serverColor.copy(alpha = 0.25f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Category Badge
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = serverColor.copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(serverColor, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = fav.categoryLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = serverColor
                                            )
                                        }
                                    }

                                    // Delete action button
                                    IconButton(
                                        onClick = { favoritePendingRemoval = fav },
                                        enabled = fav.emotionId !in serverState.removingEmotionIds,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        if (fav.emotionId in serverState.removingEmotionIds) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "${fav.emotionName} 즐겨찾기 해제",
                                                tint = serverColor
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Emotion word title
                                    Text(
                                        text = fav.emotionName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Total counter badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "누적 ${fav.totalCount}회",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Server cumulative count (read-only until explicit correction)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "오늘 누적",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${fav.todayCount}회",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = serverColor
                                        )
                                    }
                                    TextButton(
                                        enabled = fav.emotionId !in serverState.correctingEmotionIds,
                                        onClick = {
                                            correctionValue = fav.todayCount
                                            correctionPending = fav
                                        }
                                    ) { Text("수정", color = serverColor) }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Count added by this one new event
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            serverColor.copy(alpha = 0.08f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "이번에 느낀 횟수",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Minus Button
                                        IconButton(
                                            onClick = { viewModel.decrementServerFavorite(fav.emotionId) },
                                            enabled = fav.emotionId !in serverState.addingEmotionIds,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Decrement",
                                                tint = serverColor
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Text(
                                            text = "${serverState.addCounts[fav.emotionId] ?: 1}회",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = serverColor
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                         // Plus Button
                                        IconButton(
                                            onClick = { viewModel.incrementServerFavorite(fav.emotionId) },
                                            enabled = fav.emotionId !in serverState.addingEmotionIds,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(serverColor, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Increment",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val addCount = serverState.addCounts[fav.emotionId] ?: 1
                                Button(
                                    onClick = { viewModel.addServerFavoriteEmotion(fav.emotionId) },
                                    enabled = fav.emotionId !in serverState.addingEmotionIds,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = com.example.ui.theme.AppActionButton,
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (fav.emotionId in serverState.addingEmotionIds) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Text("${addCount}회 추가 기록", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (connectedStory != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = serverColor.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, serverColor.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Text(
                                                text = "연결된 사연",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (connectedStory.selectedStoryPeriod.isBlank()) {
                                                    connectedStory.selectedStoryTitle
                                                } else {
                                                    "${connectedStory.selectedStoryTitle} · ${connectedStory.selectedStoryPeriod}"
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = serverColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "관련 정도 ${connectedStory.linkStrength}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = serverColor
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // Button to select / connect my story (나의 사연 선택)
                                OutlinedButton(
                                    onClick = {
                                        selectedFavForStory = fav
                                        viewModel.loadEmotionStoryOptions(fav.emotionId)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, serverColor.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = serverColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (connectedStory == null) "📖 나의 사연 선택하기" else "📖 사연 변경하기",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = serverColor
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }

    }

    favoritePendingRemoval?.let { favorite ->
        val isRemoving = favorite.emotionId in serverState.removingEmotionIds
        AlertDialog(
            onDismissRequest = { if (!isRemoving) favoritePendingRemoval = null },
            title = { Text("즐겨찾기 해제") },
            text = { Text("\"${favorite.emotionName}\" 감정을 즐겨찾기에서 해제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    enabled = !isRemoving,
                    onClick = {
                        viewModel.removeServerFavorite(favorite.emotionId) {
                            favoritePendingRemoval = null
                        }
                    }
                ) { Text("해제") }
            },
            dismissButton = {
                TextButton(enabled = !isRemoving, onClick = { favoritePendingRemoval = null }) {
                    Text("취소")
                }
            }
        )
    }

    correctionPending?.let { favorite ->
        val isCorrecting = favorite.emotionId in serverState.correctingEmotionIds
        AlertDialog(
            onDismissRequest = { if (!isCorrecting) correctionPending = null },
            title = { Text("오늘 누적 수정") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${favorite.emotionName}의 오늘 누적 횟수")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            enabled = !isCorrecting && correctionValue > 0,
                            onClick = { correctionValue = (correctionValue - 1).coerceAtLeast(0) }
                        ) { Icon(Icons.Default.Remove, contentDescription = "누적 횟수 감소") }
                        Text(
                            text = "${correctionValue}회",
                            modifier = Modifier.padding(horizontal = 20.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            enabled = !isCorrecting && correctionValue < 999,
                            onClick = { correctionValue = (correctionValue + 1).coerceAtMost(999) }
                        ) { Icon(Icons.Default.Add, contentDescription = "누적 횟수 증가") }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isCorrecting,
                    onClick = {
                        viewModel.correctServerFavoriteEmotion(favorite.emotionId, correctionValue) {
                            correctionPending = null
                        }
                    }
                ) { Text(if (isCorrecting) "저장 중" else "수정 저장") }
            },
            dismissButton = {
                TextButton(enabled = !isCorrecting, onClick = { correctionPending = null }) { Text("취소") }
            }
        )
    }

    // Dialog to add new custom favorite emotion
    if (showAddDialog) {
        var wordInput by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf(EmotionCategory.JOY) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("자주 느끼는 감정 추가") },
            text = {
                Column {
                    Text(
                        text = "감정 단어와 영역을 선택하세요.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = wordInput,
                        onValueChange = { wordInput = it },
                        label = { Text("감정 단어 (예: 뿌듯한, 가슴찡한)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "감정 영역 선택:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EmotionCategory.entries.forEach { cat ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = if (selectedCategory == cat) cat.color else MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat.hanja.substring(0, 1),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCategory == cat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.AppActionButton,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    onClick = {
                        if (wordInput.isNotBlank()) {
                            viewModel.addCustomFavorite(wordInput.trim(), selectedCategory)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // Dialog to select or create a story connected to a favorite emotion
    if (selectedFavForStory != null) {
        val fav = selectedFavForStory!!
        val serverColor = EmotionColors.forCategoryCode(
            fav.categoryCode,
            parseServerColor(fav.colorCode)
        )
        val options = storyState.options
        var selectedStoryId by remember(options?.selected?.storyId, fav.emotionId) {
            mutableStateOf(options?.selected?.storyId)
        }
        var relevanceLevel by remember(options?.selected?.linkStrength, fav.emotionId) {
            mutableStateOf(options?.selected?.linkStrength)
        }
        var validationMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                selectedFavForStory = null
                viewModel.clearEmotionStory()
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "이 감정이 어떤 기억과 관련되어 있나요?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        selectedFavForStory = null
                        viewModel.clearEmotionStory()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = options?.record?.let { "${it.categoryLabel} · ${it.emotionName} · 오늘 ${it.count}회" }
                            ?: "${fav.categoryLabel} · ${fav.emotionName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = serverColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    if (storyState.isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (storyState.error != null) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(storyState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { viewModel.loadEmotionStoryOptions(fav.emotionId) }) { Text("다시 시도") }
                        }
                    } else if (options != null && options.stories.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(options.stories, key = { it.storyId }) { story ->
                                val isSelected = selectedStoryId == story.storyId
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFEAF2FB)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF74AFDD) else MaterialTheme.colorScheme.outline
                                    ),
                                    onClick = { selectedStoryId = story.storyId }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedStoryId = story.storyId }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (story.period.isBlank()) story.title else "${story.title} · ${story.period}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (options != null) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("등록된 나의 사연이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("관련 정도", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (showPageTitle) Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..5).forEach { level ->
                            val selected = relevanceLevel == level
                            OutlinedButton(
                                onClick = {
                                    relevanceLevel = level
                                    validationMessage = null
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) Color(0xFF74AFDD) else MaterialTheme.colorScheme.surface,
                                    contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, if (selected) Color(0xFF74AFDD) else MaterialTheme.colorScheme.outline)
                            ) {
                                Text(level.toString())
                            }
                        }
                    }
                    validationMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.AppActionButton,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    onClick = {
                        when {
                            selectedStoryId == null -> validationMessage = "연결할 사연을 선택해주세요."
                            relevanceLevel == null -> validationMessage = "관련 정도를 선택해주세요."
                            else -> {
                                viewModel.linkEmotionStory(fav.emotionId, selectedStoryId!!, relevanceLevel!!) {
                                    selectedFavForStory = null
                                    viewModel.clearEmotionStory()
                                }
                            }
                        }
                    }
                ) {
                    Text("연결 저장")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        selectedFavForStory = null
                        viewModel.clearEmotionStory()
                        onOpenInnerStories()
                    }
                ) {
                    Text("+ 새로운 사연 등록")
                }
            }
        )
    }
}

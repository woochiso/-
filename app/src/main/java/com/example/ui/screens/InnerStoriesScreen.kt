package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppActionButton
import com.example.ui.viewmodel.StoryViewModel

@Composable
fun InnerStoriesScreen(viewModel: StoryViewModel, modifier: Modifier = Modifier, initialStoryId: Long? = null, onInitialStoryHandled: () -> Unit = {}, showPageTitle: Boolean = true) {
    val state by viewModel.state.collectAsState()
    var editor by remember { mutableStateOf(false) }
    var editorMutationVersion by remember { mutableIntStateOf(state.mutationVersion) }
    var pendingStoryId by remember { mutableStateOf<Long?>(null) }
    var deleteId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { viewModel.clearEditing(); viewModel.load(); initialStoryId?.let { pendingStoryId = it; viewModel.detail(it); onInitialStoryHandled() } }
    LaunchedEffect(state.editing, pendingStoryId) { val pending = pendingStoryId; if (pending != null && state.editing?.storyId == pending) { editor = true; pendingStoryId = null } }
    LaunchedEffect(state.mutationVersion) { if (editor && state.mutationVersion > editorMutationVersion) { editor = false; pendingStoryId = null; viewModel.clearEditing() } }
    BackHandler(enabled = editor) { editor = false; pendingStoryId = null; viewModel.clearEditing() }
    if (editor) {
        InnerStoryEditorScreen(state.editing, { editor = false; pendingStoryId = null; viewModel.clearEditing() }, { editorMutationVersion = state.mutationVersion; viewModel.create(it) }, { editorMutationVersion = state.mutationVersion; viewModel.update(it) }, modifier)
        return
    }
    Box(modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { if (showPageTitle) Text("나의 사연", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("마음속에 남아 있는 이야기와 감정을 기록해보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (state.loading) item { CircularProgressIndicator() }
            if (!state.loading && state.stories.isEmpty()) item { Text("기록된 사연이 없습니다.", modifier = Modifier.padding(24.dp)) }
            items(state.stories, key = { it.storyId }) { story ->
                Card(modifier = Modifier.fillMaxWidth().clickable { pendingStoryId = story.storyId; viewModel.detail(story.storyId) }, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(story.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton({ deleteId = story.storyId }) { Icon(Icons.Default.Delete, "사연 삭제") } }
                        Text(story.period, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("현재 영향 ${story.currentImpact} / 5", color = AppActionButton, fontWeight = FontWeight.SemiBold)
                        Text(story.updatedAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
        FloatingActionButton({ pendingStoryId = null; editorMutationVersion = state.mutationVersion; viewModel.clearEditing(); editor = true }, Modifier.align(Alignment.BottomEnd).padding(24.dp), containerColor = AppActionButton, contentColor = Color.White) { Icon(Icons.Default.Add, "사연 작성") }
        deleteId?.let { id -> AlertDialog({ deleteId = null }, title = { Text("사연 삭제") }, text = { Text("이 사연을 삭제하시겠습니까?") }, confirmButton = { Button({ viewModel.delete(id); deleteId = null }) { Text("삭제") } }, dismissButton = { TextButton({ deleteId = null }) { Text("취소") } }) }
    }
}

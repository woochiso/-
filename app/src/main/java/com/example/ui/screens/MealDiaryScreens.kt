package com.example.ui.screens

import android.net.Uri
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.auth.TokenManager
import com.example.data.remote.dto.MealFoodDto
import com.example.data.remote.dto.MealNutritionDto
import com.example.ui.theme.AppActionButton
import com.example.ui.viewmodel.MealViewModel
import com.example.utils.MealGraphImageExporter
import java.io.File
import java.time.YearMonth

private val MealBlue = Color(0xFF74AFDD)
private val MealPale = Color(0xFFEAF2FB)

@Composable
fun MealDiaryHubScreen(onRecord: () -> Unit, onFavorites: () -> Unit, onSearch: () -> Unit, onCalendar: () -> Unit, onGraph: () -> Unit, onAnalysis: () -> Unit) {
    val cards = listOf(
        Triple("식단기록", "사진으로 오늘 식사를 기록해요.", Icons.Default.PhotoCamera) to onRecord,
        Triple("즐찾음식", "자주 먹는 식사를 빠르게 기록해요.", Icons.Default.Star) to onFavorites,
        Triple("음식찾기", "음식 이름으로 직접 찾아보세요.", Icons.Default.Search) to onSearch,
        Triple("식단달력", "날짜별 식단을 한눈에 확인해요.", Icons.Default.CalendarMonth) to onCalendar,
        Triple("식단그래프", "7가지 영양 흐름을 살펴봐요.", Icons.Default.BubbleChart) to onGraph,
        Triple("AI 식단분석", "기록된 식단을 AI가 정리해요.", Icons.Default.AutoAwesome) to onAnalysis
    )
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(8.dp)); Text("식단다이어리", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text("오늘 먹은 음식을 간단하게 기록하고\n나의 식단 흐름을 확인해보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(cards.chunked(2)) { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { (info, action) -> MealMenuCard(info.first, info.second, info.third, action, Modifier.weight(1f)) }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable private fun MealMenuCard(title: String, description: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick, modifier = modifier.height(154.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(16.dp)) { Icon(icon, null, tint = MealBlue, modifier = Modifier.size(30.dp)); Spacer(Modifier.height(12.dp)); Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun MealRecordScreen(viewModel: MealViewModel, onAuthExpired: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle(); val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }; var deleteId by remember { mutableStateOf<Long?>(null) }
    var showRepresentativeInput by remember(state.representativeCandidates) { mutableStateOf(state.representativeCandidates.isEmpty()) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::selectImage) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let(viewModel::selectImage) }
    LaunchedEffect(Unit) { viewModel.loadRecords() }; MealMessages(viewModel); LaunchedEffect(state.requiresLogin) { if (state.requiresLogin) onAuthExpired() }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(if (state.editingId == null) "오늘 뭐 드셨어요?" else "식단 기록 수정", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("사진 분석과 최종 영양 계산은 우치소 서버에서 처리합니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ val f = File(context.cacheDir, "meal_camera_${System.nanoTime()}.jpg"); val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f); cameraUri = uri; camera.launch(uri) }, Modifier.weight(1f)) { Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(5.dp)); Text("음식 사진 촬영") }
            OutlinedButton({ gallery.launch("image/*") }, Modifier.weight(1f)) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(5.dp)); Text("갤러리 선택") }
        } }
        val preview = state.selectedImage ?: state.editingImageUrl
        if (preview != null) item { Card(colors = CardDefaults.cardColors(containerColor = MealPale)) { Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { MealImage(preview, "식단 사진 미리보기", Modifier.fillMaxWidth().height(220.dp)); Spacer(Modifier.height(8.dp)); Text(if (state.selectedImage != null) "선택한 사진" else "현재 저장된 사진", fontWeight = FontWeight.Bold); if (state.selectedImage != null) Button(viewModel::analyze.bind(context), enabled = !state.isAnalyzing) { Text(if (state.isAnalyzing) "분석 중..." else "사진 분석하기") } } } }
        if (state.isAnalyzing) item { LoadingCard("음식 사진을 분석하고 있습니다.\nAI 음식 확인 → 공공 영양데이터 확인 → 예상 영양성분 계산") }
        if (state.analysisMealType == "single" && (state.representativeCandidates.isNotEmpty() || state.representativeFood.isNotBlank())) item {
            Card(border = BorderStroke(1.dp, MealBlue), colors = CardDefaults.cardColors(containerColor = MealPale)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("대표 음식 선택", fontWeight = FontWeight.Bold)
                    state.representativeCandidates.take(3).forEach { candidate ->
                        FilterChip(
                            selected = candidate.name == state.representativeFood,
                            onClick = { showRepresentativeInput = false; viewModel.selectRepresentative(candidate.name) },
                            label = { Text(candidate.name) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedButton({ showRepresentativeInput = true }, Modifier.fillMaxWidth()) { Text("직접 입력") }
                    if (showRepresentativeInput) {
                        OutlinedTextField(
                            value = state.representativeFood,
                            onValueChange = viewModel::setRepresentativeInput,
                            label = { Text("실제 대표 음식명") },
                            supportingText = { Text("입력 후 아래 예상 영양성분 확인을 눌러주세요.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (state.uncertainItems.isNotEmpty()) Text(
                        "확인이 필요한 내용: ${state.uncertainItems.joinToString(" · ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (state.foods.isNotEmpty()) item { Text("AI가 찾은 음식", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(state.foods.size) { index -> FoodEditor(state.foods[index], { transform -> viewModel.updateFood(index, transform) }) }
        if (state.foods.isNotEmpty()) {
            item { MealMetaFields(state.mealDate, state.mealTime, state.mealType, state.waterMl, state.memo, viewModel::setDate, viewModel::setTime, viewModel::setType, viewModel::setWater, viewModel::setMemo) }
            item { OutlinedButton(viewModel::calculateNutrition, Modifier.fillMaxWidth()) { Text("예상 영양성분 확인") } }
            state.nutrition?.let { item { NutritionCard(it) } }
            item { Button(viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) { Text(if (state.isSaving) "저장 중..." else if (state.editingId == null) "저장하기" else "수정 저장하기") } }
        }
        state.editorError?.let { item { ErrorCard(it) { viewModel.consumeEditorError() } } }
        state.recordError?.let { item { ErrorCard(it, viewModel::loadRecords) } }
        item { HorizontalDivider(); Text("최근 식단 기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(state.records, key = { it.id }) { record -> Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(14.dp)) { if (record.hasImage && !record.imageUrl.isNullOrBlank()) MealImage(record.imageUrl, "${record.foodName} 식단 사진", Modifier.fillMaxWidth().height(170.dp)) else Box(Modifier.fillMaxWidth().height(100.dp).background(MealPale, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("사진 없는 식사", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Spacer(Modifier.height(8.dp)); Text("${mealTypeLabel(record.type)} · ${record.date}", fontWeight = FontWeight.Bold); Text(record.foodName); Row { TextButton({ viewModel.edit(record.id) }) { Text("수정") }; TextButton({ deleteId = record.id }) { Text("삭제", color = MaterialTheme.colorScheme.error) } } } } }
        item { Spacer(Modifier.height(12.dp)) }
    }
    if (deleteId != null) AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("이 식단 기록을 삭제하시겠습니까?") }, confirmButton = { TextButton({ deleteId?.let(viewModel::delete); deleteId = null }) { Text("삭제", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ deleteId = null }) { Text("취소") } })
}

private fun ((Context) -> Unit).bind(context: Context): () -> Unit = { invoke(context) }

@Composable private fun FoodEditor(food: MealFoodDto, update: (((MealFoodDto) -> MealFoodDto)) -> Unit) {
    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(food.included, { checked -> update { it.copy(included = checked) } }); OutlinedTextField(food.name, { name -> update { it.copy(name = name.take(120)) } }, label = { Text("음식명") }, modifier = Modifier.weight(1f), singleLine = true) }
        if (food.included) if (food.portionType == "COUNT") Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { TextButton({ update { it.copy(count = ((it.count ?: 1) - 1).coerceAtLeast(1), amount = ((it.count ?: 1) - 1).coerceAtLeast(1).toString()) } }) { Text("−") }; Text("${food.count ?: food.amount.toIntOrNull() ?: 1}개", fontWeight = FontWeight.Bold); TextButton({ update { it.copy(count = ((it.count ?: 1) + 1).coerceAtMost(100), amount = ((it.count ?: 1) + 1).coerceAtMost(100).toString()) } }) { Text("+") } }
        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("small" to "적게", "normal" to "보통", "large" to "많이").forEach { (value, label) -> FilterChip(food.portionLevel == value, { update { it.copy(portionLevel = value) } }, { Text(label) }, modifier = Modifier.weight(1f)) } }
    } }
}

@Composable private fun MealMetaFields(date: String, time: String, type: String, water: String, memo: String, onDate: (String) -> Unit, onTime: (String) -> Unit, onType: (String) -> Unit, onWater: (String) -> Unit, onMemo: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(date, onDate, label = { Text("날짜") }, modifier = Modifier.weight(1f)); OutlinedTextField(time, onTime, label = { Text("시간") }, modifier = Modifier.weight(1f)) }; Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("breakfast" to "아침", "lunch" to "점심", "dinner" to "저녁", "snack" to "간식").forEach { (v, l) -> FilterChip(type.equals(v, true), { onType(v) }, { Text(l) }, modifier = Modifier.weight(1f)) } }; OutlinedTextField(water, onWater, label = { Text("물 섭취량 (ml)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()); OutlinedTextField(memo, onMemo, label = { Text("메모") }, modifier = Modifier.fillMaxWidth()) }
}

@Composable private fun NutritionCard(n: MealNutritionDto) { Card(colors = CardDefaults.cardColors(containerColor = MealPale)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("예상 영양성분", fontWeight = FontWeight.Bold); val labels = linkedMapOf("calories" to "🔥 열량 (kcal)", "carbohydrate" to "🍚 탄수화물 (g)", "protein" to "🥩 단백질 (g)", "fat" to "🥑 지방 (g)", "fiber" to "🥬 식이섬유 (g)"); labels.forEach { (k, label) -> Text("$label  ${n.summaryValue(k)?.let { "%.1f".format(it) } ?: "정보 없음"}") }; n.stateEntries().forEach { (k, v) -> Text("$k · $v", style = MaterialTheme.typography.bodySmall) }; if (n.reliability == "STANDARD_ESTIMATE" || n.source?.contains("STANDARD_ESTIMATE") == true) Text("사진만으로 정확한 중량을 측정할 수 없어 표준 평균중량을 기준으로 계산합니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); n.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) } } }
}

@Composable fun MealFavoritesScreen(viewModel: MealViewModel, onAuthExpired: () -> Unit) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.loadFavorites() }
    MealMessages(viewModel, snackbar)
    LaunchedEffect(s.requiresLogin) { if (s.requiresLogin) onAuthExpired() }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("즐찾음식", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("자주 먹는 음식을 사진 없이 빠르게 기록해보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(s.favoriteTemplates, key = { "template:${it.id}" }) { t ->
                SimpleActionCard("⭐ ${t.name}", t.foods.joinToString { "${it.name} ${it.amount}${it.unit}" }, "바로 기록", s.quickRecordingFavoriteId == "template:${t.id}") { viewModel.quickRecord(templateId = t.id) }
            }
            items(s.favoriteFoods, key = { "food:${it.id}" }) { f ->
                SimpleActionCard("⭐ ${f.name}", f.category.orEmpty(), "바로 기록", s.quickRecordingFavoriteId == "food:${f.id}") { viewModel.quickRecord(foodId = f.id) }
            }
            s.favoriteError?.let { item { ErrorCard(it, viewModel::loadFavorites) } }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}

@Composable fun MealSearchScreen(viewModel: MealViewModel, onAuthExpired: () -> Unit) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MealMessages(viewModel, snackbar)
    LaunchedEffect(s.requiresLogin) { if (s.requiresLogin) onAuthExpired() }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("음식찾기", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item { Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(s.searchQuery, viewModel::setQuery, label = { Text("음식 이름") }, modifier = Modifier.weight(1f), singleLine = true); IconButton(viewModel::search) { Icon(Icons.Default.Search, "검색") } } }
            items(s.searchResults) { f -> Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(f.name, fontWeight = FontWeight.Bold); Text(if (f.portionType == "COUNT") "개수형 음식" else "분량형 음식", style = MaterialTheme.typography.bodySmall) }; IconButton({ viewModel.toggleFavorite(f) }) { Icon(if (f.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, "즐겨찾기", tint = MealBlue) }; TextButton({ viewModel.quickRecord(foodId = f.id, fromSearch = true) }) { Text("바로 기록") } } } }
            s.searchError?.let { item { ErrorCard(it, viewModel::search) } }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}

@Composable fun MealCalendarScreen(viewModel: MealViewModel, onAuthExpired: () -> Unit) { val s by viewModel.state.collectAsStateWithLifecycle(); LaunchedEffect(Unit) { viewModel.loadCalendar() }; LaunchedEffect(s.requiresLogin) { if (s.requiresLogin) onAuthExpired() }; LazyColumn(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("식단달력", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { IconButton({ viewModel.setCalendarMonth(YearMonth.parse(s.calendarMonth).minusMonths(1).toString()) }) { Icon(Icons.Default.ChevronLeft, null) }; Text(s.calendarMonth, fontWeight = FontWeight.Bold); IconButton({ viewModel.setCalendarMonth(YearMonth.parse(s.calendarMonth).plusMonths(1).toString()) }) { Icon(Icons.Default.ChevronRight, null) } } }; items(s.calendarRecords.groupBy { it.date }.toList()) { (date, records) -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) { Column(Modifier.padding(14.dp)) { Text(date, fontWeight = FontWeight.Bold, color = MealBlue); records.forEach { Text("${mealTypeLabel(it.type)} · ${it.foodName}") } } } }; if (!s.isLoading && s.calendarRecords.isEmpty() && s.calendarError == null) item { Text("이 달에 기록된 식단이 없습니다.") }; s.calendarError?.let { item { ErrorCard(it, viewModel::loadCalendar) } } } }

@Composable fun MealGraphScreen(viewModel: MealViewModel, onAuthExpired: () -> Unit) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var saveMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.loadGraph() }
    LaunchedEffect(s.requiresLogin) { if (s.requiresLogin) onAuthExpired() }
    LazyColumn(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Text("${s.graph?.nickname?.takeIf(String::isNotBlank) ?: "나"}의 식단 영양 균형", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { PeriodChips(s.period) { viewModel.setPeriod(it); viewModel.loadGraph() } }
        item {
            val nutrients = s.graph?.nutrients.orEmpty()
            BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
                val graphWidth = maxWidth
                val center = graphWidth / 2
                val radius = (graphWidth / 2 - 45.dp).coerceAtLeast(82.dp)
                nutrients.take(7).forEachIndexed { index, n ->
                    val angle = Math.toRadians(-90.0 + index * (360.0 / 7.0))
                    val mobileSize = (68f + (s.graph?.bubbleScale(n.valid) ?: 0.5f) * 24f).dp
                    val serverSize = mobileSize
                    val x = center + radius * kotlin.math.cos(angle).toFloat() - serverSize / 2
                    val y = center + radius * kotlin.math.sin(angle).toFloat() - serverSize / 2
                    Box(
                        Modifier.offset(x, y).size(serverSize).background(mealNutrientColor(n.key), CircleShape).padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(n.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White); Text(n.displayState, style = MaterialTheme.typography.labelSmall, color = Color.White); Text("${n.valid}끼", style = MaterialTheme.typography.labelSmall, color = Color.White) } }
                }
                if (nutrients.isNotEmpty()) Box(Modifier.align(Alignment.Center).size(82.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Text("영양\n균형", fontWeight = FontWeight.Bold, color = Color(0xFF064AD6)) }
            }
        }
        item { Text("그래프의 상태와 크기는 서버 집계 결과를 사용합니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        s.graph?.let { graph ->
            item { Button(onClick = { saveMessage = if (MealGraphImageExporter.save(context, graph)) "식단그래프 이미지를 저장했습니다." else "이미지를 저장하지 못했습니다." }, colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("이미지 저장") } }
            saveMessage?.let { message -> item { Text(message, color = if (message.contains("저장했습니다")) MealBlue else MaterialTheme.colorScheme.error) } }
            item { Text("자주 먹는 음식 TOP 20", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth()) }
            val foods = graph.frequentFoodItems()
            if (foods.isEmpty()) item { Text("선택한 기간에 기록된 음식이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(foods) { food -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("${foods.indexOf(food) + 1}. ${food.name}"); Text(food.count?.let { "${it}회" } ?: "") } }
        }
        s.graphError?.let { item { ErrorCard(it, viewModel::loadGraph) } }
    }
}

private fun mealNutrientColor(key: String): Color = when (key) { "carbohydrate" -> Color(0xFF5F8FDB); "protein" -> Color(0xFF4FA58D); "fat" -> Color(0xFF7C78C8); "fiber" -> Color(0xFF67A96B); "vitamin" -> Color(0xFFDF9A43); "mineral" -> Color(0xFF718DA5); "water" -> Color(0xFF4B9FCA); else -> MealBlue }

@Composable private fun MealImage(model: Any, description: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(model) {
        ImageRequest.Builder(context).data(model).apply {
            if (model is String && model.contains("/api/meal/image.php")) {
                TokenManager(context).accessToken()?.let { addHeader("Authorization", "Bearer $it") }
            }
        }.build()
    }
    SubcomposeAsyncImage(model = request, contentDescription = description, modifier = modifier, contentScale = androidx.compose.ui.layout.ContentScale.Crop, loading = { Box(Modifier.fillMaxSize().background(MealPale), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) } }, error = { Box(Modifier.fillMaxSize().background(MealPale), contentAlignment = Alignment.Center) { Text("사진을 불러오지 못했습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }, success = { SubcomposeAsyncImageContent() })
}

@Composable private fun MealMessages(viewModel: MealViewModel, snackbarHostState: SnackbarHostState? = null) {
    val context = LocalContext.current
    LaunchedEffect(viewModel, snackbarHostState) { viewModel.messages.collect { message ->
        if (snackbarHostState != null) snackbarHostState.showSnackbar(message)
        else Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    } }
}

@Composable fun MealAnalysisScreen(viewModel: MealViewModel, onAuthExpired: () -> Unit) { val s by viewModel.state.collectAsStateWithLifecycle(); LaunchedEffect(Unit) { viewModel.loadAnalysis() }; LaunchedEffect(s.requiresLogin) { if (s.requiresLogin) onAuthExpired() }; LazyColumn(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("AI 식단분석", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }; item { PeriodChips(s.period) { viewModel.setPeriod(it); viewModel.loadAnalysis() } }; item { Button(viewModel::loadAnalysis, enabled = !s.isAnalysisLoading, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) { Text(if (s.isAnalysisLoading) "분석 중..." else "AI 식단분석") } }; if (s.isAnalysisLoading) item { Card(colors = CardDefaults.cardColors(containerColor = MealPale)) { Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(); Text("AI가 식단을 분석하고 있습니다...", fontWeight = FontWeight.Bold); Text("기록된 식단을 바탕으로 분석 중입니다. 잠시만 기다려주세요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }; if (!s.isAnalysisLoading) s.aiAnalysis?.let { a -> item { Card(colors = CardDefaults.cardColors(containerColor = MealPale)) { Column(Modifier.padding(16.dp)) { Text(a.summary, fontWeight = FontWeight.Bold); a.patterns.forEach { Text("• $it") } } } }; item { Text("실천 제안", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); a.suggestions.forEach { Text("• $it") } }; item { Text(a.oneLine, color = MealBlue, fontWeight = FontWeight.Bold) } }; item { Text("AI 분석은 의료 진단·질병 판정·치료 지시가 아니며, 기록된 식단을 바탕으로 한 참고 정보입니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; s.analysisError?.let { item { ErrorCard(it, viewModel::loadAnalysis) } } } }

@Composable private fun PeriodChips(selected: String, onSelect: (String) -> Unit) { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("7d" to "7일", "30d" to "30일", "90d" to "3개월", "all" to "전체").forEach { (v, l) -> FilterChip(selected == v, { onSelect(v) }, { Text(l) }, modifier = Modifier.weight(1f)) } } }
@Composable private fun SimpleActionCard(title: String, detail: String, action: String, isLoading: Boolean = false, onClick: () -> Unit) { Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall); Button(onClick, Modifier.align(Alignment.End), enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) { if (isLoading) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(7.dp)); Text("기록 중...") } else Text(action) } } } }
@Composable private fun LoadingCard(text: String) { Card(colors = CardDefaults.cardColors(containerColor = MealPale)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(28.dp)); Spacer(Modifier.width(12.dp)); Text(text) } } }
@Composable private fun ErrorCard(text: String, retry: () -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(text, color = MaterialTheme.colorScheme.onErrorContainer); TextButton(retry) { Text("다시 시도") } } } }
private fun mealTypeLabel(value: String) = when (value.uppercase()) { "BREAKFAST" -> "🌅 아침"; "LUNCH" -> "☀️ 점심"; "DINNER" -> "🌙 저녁"; else -> "🍎 간식" }

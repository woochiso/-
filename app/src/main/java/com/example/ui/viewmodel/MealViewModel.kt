package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.*
import com.example.data.repository.EmotionServerResult
import com.example.data.repository.MealRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class MealUiState(
    val isLoading: Boolean = false, val isAnalyzing: Boolean = false, val isSaving: Boolean = false,
    val editorError: String? = null, val recordError: String? = null,
    val favoriteError: String? = null, val searchError: String? = null,
    val calendarError: String? = null, val graphError: String? = null,
    val analysisError: String? = null, val isAnalysisLoading: Boolean = false, val requiresLogin: Boolean = false,
    val records: List<MealRecordDto> = emptyList(), val foods: List<MealFoodDto> = emptyList(),
    val nutrition: MealNutritionDto? = null, val selectedImage: Uri? = null, val uploadFile: File? = null,
    val editingImageUrl: String? = null,
    val representativeFood: String = "", val analysisMealType: String = "single",
    val representativeCandidates: List<MealRepresentativeCandidateDto> = emptyList(),
    val uncertainItems: List<String> = emptyList(),
    val mealDate: String = LocalDate.now().toString(), val mealTime: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val mealType: String = "breakfast", val waterMl: String = "0", val memo: String = "", val editingId: Long? = null,
    val searchQuery: String = "", val searchResults: List<MealSearchItemDto> = emptyList(),
    val favoriteFoods: List<MealSearchItemDto> = emptyList(), val favoriteTemplates: List<MealFavoriteTemplateDto> = emptyList(),
    val quickRecordingFavoriteId: String? = null,
    val calendarMonth: String = YearMonth.now().toString(), val calendarRecords: List<MealRecordDto> = emptyList(),
    val period: String = "7d", val graph: MealGraphResponse? = null, val aiAnalysis: MealAiAnalysisResponse? = null
)

class MealViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = MealRepository(RetrofitClient.apiService, TokenManager(application))
    private val _state = MutableStateFlow(MealUiState())
    val state = _state.asStateFlow()
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    fun loadRecords() = launch(error = { _state.value = _state.value.copy(recordError = it) }, call = { repo.records() }, success = { response -> _state.value = _state.value.copy(records = response.records) })
    fun selectImage(uri: Uri) { _state.value = _state.value.copy(selectedImage = uri, editorError = null) }
    fun setDate(v: String) { _state.value = _state.value.copy(mealDate = v.take(10)) }
    fun setTime(v: String) { _state.value = _state.value.copy(mealTime = v.take(5)) }
    fun setType(v: String) { _state.value = _state.value.copy(mealType = v) }
    fun setWater(v: String) { _state.value = _state.value.copy(waterMl = v.filter(Char::isDigit).take(5)) }
    fun setMemo(v: String) { _state.value = _state.value.copy(memo = v.take(500)) }
    fun setQuery(v: String) { _state.value = _state.value.copy(searchQuery = v) }

    fun analyze(context: Context) {
        val uri = _state.value.selectedImage ?: return
        if (_state.value.isAnalyzing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isAnalyzing = true, editorError = null)
            val file = runCatching { prepareImage(context, uri) }.getOrElse {
                _state.value = _state.value.copy(isAnalyzing = false, editorError = "사진을 준비하지 못했습니다."); return@launch
            }
            when (val result = repo.analyze(file)) {
                is EmotionServerResult.Success -> {
                    val body = result.value
                    val selectedRepresentative = body.representativeFood.ifBlank { body.representativeCandidates.firstOrNull()?.name.orEmpty() }
                    _state.value = _state.value.copy(isAnalyzing = false, uploadFile = file, foods = body.foods, nutrition = body.nutrition, representativeFood = selectedRepresentative, representativeCandidates = body.representativeCandidates.take(3), uncertainItems = body.uncertainItems.take(8), analysisMealType = body.mealType, editorError = body.message.takeIf { !body.success })
                }
                EmotionServerResult.Unauthorized -> unauthorized()
                is EmotionServerResult.Error -> _state.value = _state.value.copy(isAnalyzing = false, editorError = result.message)
            }
        }
    }

    fun updateFood(index: Int, transform: (MealFoodDto) -> MealFoodDto) {
        val list = _state.value.foods.toMutableList(); if (index !in list.indices) return
        list[index] = transform(list[index]); _state.value = _state.value.copy(foods = list, nutrition = null)
    }

    fun selectRepresentative(name: String, recalculate: Boolean = true) {
        val selected = name.trim().take(200)
        if (selected.isEmpty()) return
        val state = _state.value
        val foods = state.foods.toMutableList()
        if (foods.isNotEmpty()) foods[0] = foods[0].copy(name = selected)
        _state.value = state.copy(representativeFood = selected, foods = foods, nutrition = null)
        if (recalculate && foods.isNotEmpty()) calculateNutrition()
    }

    fun setRepresentativeInput(name: String) {
        val value = name.take(200)
        val state = _state.value
        val foods = state.foods.toMutableList()
        if (foods.isNotEmpty()) foods[0] = foods[0].copy(name = value)
        _state.value = state.copy(representativeFood = value, foods = foods, nutrition = null)
    }

    fun calculateNutrition() {
        val s = _state.value; val included = s.foods.filter { it.included }
        if (included.isEmpty()) { _state.value = s.copy(editorError = "포함할 음식을 한 개 이상 선택해주세요."); return }
        if (s.representativeFood.isBlank()) { _state.value = s.copy(editorError = "대표 음식 후보를 선택하거나 실제 음식명을 입력해주세요."); return }
        launch(error = { _state.value = _state.value.copy(editorError = it) }, call = { repo.nutrition(MealFoodsRequest(included, s.representativeFood, s.analysisMealType, s.waterMl.toIntOrNull() ?: 0)) }, success = {
            _state.value = _state.value.copy(nutrition = it.nutrition, editorError = it.message.takeIf { _ -> !it.success })
        })
    }

    fun save() {
        val s = _state.value; val included = s.foods.filter { it.included }
        if (included.isEmpty()) { _state.value = s.copy(editorError = "포함할 음식을 한 개 이상 선택해주세요."); return }
        if (s.representativeFood.isBlank()) { _state.value = s.copy(editorError = "대표 음식 후보를 선택하거나 실제 음식명을 입력해주세요."); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, editorError = null)
            when (val result = repo.save(s.uploadFile, s.editingId, s.mealDate, s.mealTime, s.mealType, s.waterMl.toIntOrNull() ?: 0, s.memo, s.analysisMealType, s.representativeFood, included)) {
                is EmotionServerResult.Success -> { _messages.emit("식단이 저장되었습니다."); resetEditor(); loadRecords() }
                EmotionServerResult.Unauthorized -> unauthorized()
                is EmotionServerResult.Error -> {
                    val message = when {
                        result.status == 0 -> "인터넷 연결을 확인해주세요."
                        result.status in 500..599 || result.status == -1 -> "식단을 저장하지 못했습니다. 잠시 후 다시 시도해주세요."
                        else -> result.message
                    }
                    _state.value = _state.value.copy(isSaving = false, editorError = message)
                }
            }
        }
    }

    fun edit(id: Long) = launch(error = { _state.value = _state.value.copy(recordError = it) }, call = { repo.detail(id) }, success = { response -> response.records.firstOrNull()?.let { r ->
        _state.value = _state.value.copy(editingId = r.id, mealDate = r.date, mealTime = r.time?.take(5) ?: "", mealType = r.type, waterMl = r.waterMl.toString(), memo = r.memo.orEmpty(), foods = r.foods, representativeFood = r.foodName, representativeCandidates = emptyList(), uncertainItems = emptyList(), nutrition = r.nutrition, selectedImage = null, uploadFile = null, editingImageUrl = r.imageUrl)
    } })
    fun delete(id: Long) = launch(error = { _state.value = _state.value.copy(recordError = it) }, call = { repo.delete(id) }, success = { _messages.emit(it.message ?: "식단 기록을 삭제했습니다."); loadRecords() })
    fun resetEditor() { _state.value = MealUiState(records = _state.value.records, favoriteFoods = _state.value.favoriteFoods, favoriteTemplates = _state.value.favoriteTemplates) }
    fun search() = launch(error = { _state.value = _state.value.copy(searchError = it) }, call = { repo.search(_state.value.searchQuery) }, success = { _state.value = _state.value.copy(searchResults = it.foods) })
    fun loadFavorites() = launch(error = { _state.value = _state.value.copy(favoriteError = it) }, call = { repo.favorites() }, success = { _state.value = _state.value.copy(favoriteFoods = it.foods, favoriteTemplates = it.templates) })
    fun toggleFavorite(item: MealSearchItemDto) = launch(error = { _state.value = _state.value.copy(searchError = it) }, call = { repo.toggleFavorite(item.id, !item.isFavorite) }, success = { response ->
        _state.value = _state.value.copy(searchResults = _state.value.searchResults.map { if (it.id == item.id) it.copy(isFavorite = !item.isFavorite) else it })
        val notice = if (response.message?.contains("이미") == true) response.message
        else if (item.isFavorite) "즐찾음식에서 삭제되었습니다." else "즐찾음식에 추가되었습니다."
        _messages.emit(notice)
        loadFavorites()
    })
    fun quickRecord(templateId: Long? = null, foodId: Long? = null, fromSearch: Boolean = false) {
        val requestId = when {
            templateId != null -> "template:$templateId"
            foodId != null -> "food:$foodId"
            else -> return
        }
        if (_state.value.quickRecordingFavoriteId != null) return
        _state.value = if (fromSearch) {
            _state.value.copy(quickRecordingFavoriteId = requestId, searchError = null)
        } else {
            _state.value.copy(quickRecordingFavoriteId = requestId, favoriteError = null)
        }
        viewModelScope.launch {
            when (val result = repo.quickRecord(templateId, foodId)) {
                is EmotionServerResult.Success -> {
                    _state.value = _state.value.copy(quickRecordingFavoriteId = null)
                    _messages.emit("식단에 기록되었습니다.")
                    loadRecords()
                }
                EmotionServerResult.Unauthorized -> {
                    _state.value = _state.value.copy(quickRecordingFavoriteId = null)
                    unauthorized()
                }
                is EmotionServerResult.Error -> {
                    _state.value = if (fromSearch) {
                        _state.value.copy(quickRecordingFavoriteId = null, searchError = result.message)
                    } else {
                        _state.value.copy(quickRecordingFavoriteId = null, favoriteError = result.message)
                    }
                }
            }
        }
    }
    fun setCalendarMonth(month: String) { _state.value = _state.value.copy(calendarMonth = month); loadCalendar() }
    fun loadCalendar() = launch(error = { _state.value = _state.value.copy(calendarError = it) }, call = { repo.calendar(_state.value.calendarMonth) }, success = { _state.value = _state.value.copy(calendarRecords = it.records) })
    fun setPeriod(period: String) { _state.value = _state.value.copy(period = period) }
    fun loadGraph() = launch(error = { _state.value = _state.value.copy(graphError = it) }, call = { repo.graph(_state.value.period) }, success = { _state.value = _state.value.copy(graph = it) })
    fun loadAnalysis() {
        if (_state.value.isAnalysisLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isAnalysisLoading = true, analysisError = null)
            when (val result = repo.analysis(_state.value.period)) {
                is EmotionServerResult.Success -> _state.value = _state.value.copy(isAnalysisLoading = false, aiAnalysis = result.value)
                EmotionServerResult.Unauthorized -> unauthorized()
                is EmotionServerResult.Error -> {
                    val message = when {
                        result.status in 500..599 -> "AI 식단분석 중 서버 오류가 발생했습니다."
                        result.status == -1 -> "AI 분석 결과를 처리하지 못했습니다."
                        else -> result.message
                    }
                    _state.value = _state.value.copy(isAnalysisLoading = false, analysisError = message)
                }
            }
        }
    }
    fun consumeEditorError() { _state.value = _state.value.copy(editorError = null) }

    private fun <T> launch(error: (String?) -> Unit, call: suspend () -> EmotionServerResult<T>, success: suspend (T) -> Unit) {
        viewModelScope.launch { _state.value = _state.value.copy(isLoading = true); error(null); when (val result = call()) {
            is EmotionServerResult.Success -> { _state.value = _state.value.copy(isLoading = false); success(result.value) }
            EmotionServerResult.Unauthorized -> unauthorized()
            is EmotionServerResult.Error -> { _state.value = _state.value.copy(isLoading = false); error(result.message) }
        } }
    }
    private fun unauthorized() { _state.value = _state.value.copy(isLoading = false, isAnalyzing = false, isSaving = false, isAnalysisLoading = false, quickRecordingFavoriteId = null, requiresLogin = true) }

    private suspend fun prepareImage(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val declaredMime = resolver.getType(uri)?.lowercase()
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        val declaredExtension = displayName?.substringAfterLast('.', "")?.lowercase()
        val sourceExtension = when {
            declaredMime == "image/jpeg" || declaredExtension in setOf("jpg", "jpeg") -> "jpg"
            declaredMime == "image/png" || declaredExtension == "png" -> "png"
            declaredMime == "image/webp" || declaredExtension == "webp" -> "webp"
            else -> "img"
        }
        val raw = File(context.cacheDir, "meal_source_${System.nanoTime()}.$sourceExtension")
        context.contentResolver.openInputStream(uri).use { input -> requireNotNull(input); raw.outputStream().use(input::copyTo) }
        require(raw.length() in 1..(20L * 1024 * 1024))
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(raw.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0)

        // The website sends the selected JPG/PNG/WebP to the shared server analyzer unchanged.
        // Preserve the same bytes on Android whenever they already satisfy that public contract.
        if (sourceExtension != "img" && raw.length() <= 8L * 1024 * 1024 &&
            bounds.outWidth <= 12_000 && bounds.outHeight <= 12_000 &&
            bounds.outWidth.toLong() * bounds.outHeight.toLong() <= 40_000_000L
        ) return@withContext raw

        // Oversized/unknown mobile sources are normalized only as a bounded fallback.
        var sample = 1; while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
        val bitmap = requireNotNull(BitmapFactory.decodeFile(raw.path, BitmapFactory.Options().apply { inSampleSize = sample }))
        val orientation = runCatching { ExifInterface(raw.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val degree = when (orientation) { ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f; ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f }
        val rotated = if (degree == 0f) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degree) }, true).also { bitmap.recycle() }
        val output = File(context.cacheDir, "meal_upload_${System.nanoTime()}.jpg")
        FileOutputStream(output).use { rotated.compress(Bitmap.CompressFormat.JPEG, 92, it) }; rotated.recycle(); raw.delete()
        require(output.length() in 1..(8L * 1024 * 1024)); output
    }
}

package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.*
import com.example.data.repository.EmotionServerResult
import com.example.data.repository.QuizTrainingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class QuizStage { SETUP, QUESTION, COMPLETE }

data class QuizTrainingUiState(
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val speechLoading: Boolean = false,
    val config: QuizConfigResponse? = null,
    val stage: QuizStage = QuizStage.SETUP,
    val category: String = "",
    val categoryTitle: String = "",
    val difficulty: String = "EASY",
    val difficultyTitle: String = "초급",
    val voice: String = "female_caster",
    val gameId: String? = null,
    val question: QuizQuestionDto? = null,
    val selectedAnswer: Int? = null,
    val answerResult: QuizActionResponse? = null,
    val score: Int = 0,
    val secondsLeft: Int = 20,
    val finalTotal: Int = 10,
    val speechBytes: ByteArray? = null,
    val speechToken: String? = null,
    val error: String? = null,
    val speechError: String? = null,
    val requiresLogin: Boolean = false
)

class QuizTrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuizTrainingRepository(RetrofitClient.apiService, TokenManager(application))
    private val _state = MutableStateFlow(QuizTrainingUiState())
    val state = _state.asStateFlow()
    private var pending: QuizActionRequest? = null
    private var timerJob: Job? = null

    init { loadConfig() }

    fun loadConfig() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        when (val r = repository.config()) {
            is EmotionServerResult.Success -> {
                val config = r.value
                _state.value = _state.value.copy(
                    loading = false,
                    config = config,
                    difficulty = config.defaultDifficulty,
                    difficultyTitle = config.difficulties.firstOrNull { it.id == config.defaultDifficulty }?.title.orEmpty(),
                    voice = config.defaultVoice,
                    secondsLeft = config.timeLimit,
                    finalTotal = config.questionCount
                )
            }
            EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(loading = false, requiresLogin = true)
            is EmotionServerResult.Error -> _state.value = _state.value.copy(loading = false, error = r.message)
        }
    }

    fun selectCategory(id: String) {
        if (_state.value.submitting) return
        val title = _state.value.config?.categories?.firstOrNull { it.id == id }?.title.orEmpty()
        _state.value = _state.value.copy(category = id, categoryTitle = title, error = null)
    }

    fun selectDifficulty(id: String) {
        if (_state.value.submitting) return
        val title = _state.value.config?.difficulties?.firstOrNull { it.id == id }?.title.orEmpty()
        _state.value = _state.value.copy(difficulty = id, difficultyTitle = title, error = null)
    }

    fun selectVoice(id: String) { if (!_state.value.submitting) _state.value = _state.value.copy(voice = id) }

    fun start() {
        val s = _state.value
        if (s.submitting || s.category.isBlank()) return
        execute(QuizActionRequest("start", UUID.randomUUID().toString(), category = s.category, difficulty = s.difficulty))
    }

    fun answer(index: Int) {
        val s = _state.value
        if (s.submitting || s.answerResult != null || s.gameId == null) return
        timerJob?.cancel()
        _state.value = s.copy(selectedAnswer = index)
        execute(QuizActionRequest("answer", UUID.randomUUID().toString(), gameId = s.gameId, answer = index))
    }

    fun next() {
        val s = _state.value
        if (s.submitting || s.answerResult == null || s.gameId == null) return
        execute(QuizActionRequest("next", UUID.randomUUID().toString(), gameId = s.gameId))
    }

    fun retry() { pending?.let(::execute) ?: loadConfig() }

    fun replaySpeech(token: String) = loadSpeech(token)
    fun clearSpeech() { _state.value = _state.value.copy(speechBytes = null) }

    fun challengeAgain() {
        timerJob?.cancel()
        _state.value = _state.value.copy(stage = QuizStage.SETUP, gameId = null, question = null, selectedAnswer = null,
            answerResult = null, score = 0, error = null, speechError = null, speechBytes = null, speechToken = null)
        start()
    }

    fun chooseOther() {
        timerJob?.cancel(); pending = null
        _state.value = _state.value.copy(stage = QuizStage.SETUP, category = "", categoryTitle = "", gameId = null,
            question = null, selectedAnswer = null, answerResult = null, score = 0, error = null,
            speechError = null, speechBytes = null, speechToken = null)
    }

    fun returnToStart() {
        timerJob?.cancel()
        pending = null
        val current = _state.value
        val config = current.config
        val difficulty = config?.defaultDifficulty ?: "EASY"
        _state.value = QuizTrainingUiState(
            loading = false,
            config = config,
            category = "",
            difficulty = difficulty,
            difficultyTitle = config?.difficulties?.firstOrNull { it.id == difficulty }?.title ?: "초급",
            voice = config?.defaultVoice ?: "female_caster",
            secondsLeft = config?.timeLimit ?: 20,
            finalTotal = config?.questionCount ?: 10
        )
    }

    private fun execute(request: QuizActionRequest) {
        if (_state.value.submitting) return
        pending = request
        viewModelScope.launch {
            _state.value = _state.value.copy(submitting = true, error = null)
            when (val r = repository.action(request)) {
                is EmotionServerResult.Success -> apply(request.action, r.value)
                EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(submitting = false, requiresLogin = true)
                is EmotionServerResult.Error -> _state.value = _state.value.copy(submitting = false, error = r.message)
            }
        }
    }

    private fun apply(action: String, response: QuizActionResponse) {
        if (!response.success) {
            _state.value = _state.value.copy(submitting = false, error = response.message ?: "요청을 처리하지 못했습니다.")
            return
        }
        when (action) {
            "start" -> showQuestion(response, response.gameId)
            "answer" -> {
                _state.value = _state.value.copy(submitting = false, answerResult = response, score = response.score)
                response.ttsToken?.takeIf(String::isNotBlank)?.let(::loadSpeech)
            }
            "next" -> if (response.finished) {
                timerJob?.cancel()
                _state.value = _state.value.copy(submitting = false, stage = QuizStage.COMPLETE, score = response.score,
                    finalTotal = response.total ?: _state.value.config?.questionCount ?: 10, speechBytes = null, speechToken = null)
            } else showQuestion(response, _state.value.gameId)
        }
    }

    private fun showQuestion(response: QuizActionResponse, gameId: String?) {
        _state.value = _state.value.copy(submitting = false, stage = QuizStage.QUESTION, gameId = gameId,
            question = response.question, selectedAnswer = null, answerResult = null, score = response.score,
            secondsLeft = _state.value.config?.timeLimit ?: 20, speechBytes = null, speechToken = response.ttsToken)
        response.ttsToken?.takeIf(String::isNotBlank)?.let(::loadSpeech)
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.secondsLeft > 0 && _state.value.answerResult == null) {
                delay(1000); _state.value = _state.value.copy(secondsLeft = (_state.value.secondsLeft - 1).coerceAtLeast(0))
            }
            val s = _state.value
            if (s.secondsLeft == 0 && s.answerResult == null && !s.submitting && s.gameId != null) {
                execute(QuizActionRequest("answer", UUID.randomUUID().toString(), gameId = s.gameId, answer = -1))
            }
        }
    }

    private fun loadSpeech(token: String) = viewModelScope.launch {
        _state.value = _state.value.copy(speechLoading = true, speechError = null, speechBytes = null, speechToken = token)
        when (val r = repository.speech(token, _state.value.voice)) {
            is EmotionServerResult.Success -> _state.value = _state.value.copy(speechLoading = false, speechBytes = r.value)
            EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(speechLoading = false, requiresLogin = true)
            is EmotionServerResult.Error -> _state.value = _state.value.copy(speechLoading = false, speechError = r.message)
        }
    }

    override fun onCleared() { timerJob?.cancel(); super.onCleared() }
}

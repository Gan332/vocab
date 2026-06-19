package com.vocabapp.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vocabapp.VocabApp
import com.vocabapp.data.db.entity.HistoryEntity
import com.vocabapp.data.model.Card
import com.vocabapp.data.model.LearnState
import com.vocabapp.data.repository.VocabRepository
import com.vocabapp.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LearnUiState(
    val showSetup: Boolean = true,
    val learnState: LearnState? = null,
    val showResult: Boolean = false,
    val showPause: Boolean = false,
    val resultTitle: String = "学习完成！",
    val resultBank: String = "",
    val resultTotal: Int = 0,
    val resultRemembered: Int = 0,
    val resultForgotten: Int = 0,
    val resultAccuracy: String = "0%",
    val resultDuration: String = "00:00",
    val resultAvgTime: String = "0s",
    val lastResult: ResultData? = null,
    val selectedBank: String = "",
    val selectedDirection: String = "word-first",
    val selectedMode: String = "flashcard",
    val hasSavedSession: Boolean = false,
    val savedSessionInfo: String = "",
    val quizOptions: List<QuizOption> = emptyList(),
    val typingFeedback: String = "",
    val typingIsCorrect: Boolean? = null,
    val elapsedText: String = "00:00"
)

data class QuizOption(
    val text: String,
    val isCorrect: Boolean,
    val isSelected: Boolean = false,
    val isDisabled: Boolean = false
)

data class ResultData(
    val bankName: String,
    val mode: String,
    val total: Int,
    val remembered: Int,
    val forgotten: Int,
    val accuracy: Int,
    val duration: Long
)

class LearnViewModel(
    private val repository: VocabRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var lastSessionType: String = "" // normal, wrongbook, favorites

    fun updateSelectedBank(bank: String) {
        _uiState.update { it.copy(selectedBank = bank) }
    }

    fun updateSelectedDirection(direction: String) {
        _uiState.update { it.copy(selectedDirection = direction) }
    }

    fun updateSelectedMode(mode: String) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun startSession(bankName: String = _uiState.value.selectedBank) {
        val state = _uiState.value
        val name = when {
            bankName == "__wrongbook__" -> "错题本"
            bankName == "__favorites__" -> "收藏单词"
            else -> bankName
        }

        viewModelScope.launch {
            var cards: List<Card> = emptyList()

            when {
                bankName == "__wrongbook__" -> {
                    cards = repository.getWrongBookCards(state.selectedDirection)
                    lastSessionType = "wrongbook"
                }
                bankName == "__favorites__" -> {
                    cards = repository.getFavoriteCards(state.selectedDirection)
                    lastSessionType = "favorites"
                }
                else -> {
                    cards = repository.createSessionCards(name, state.selectedDirection, state.selectedMode)
                    lastSessionType = "normal"
                }
            }

            if (cards.isEmpty()) {
                // No cards to study
                return@launch
            }

            // Shuffle cards
            cards = cards.shuffled()

            val learnState = LearnState(
                bankName = name,
                cards = cards,
                totalCards = cards.size,
                mode = state.selectedMode,
                direction = state.selectedDirection,
                isWrongBook = bankName == "__wrongbook__",
                isFavorites = bankName == "__favorites__"
            )

            _uiState.update {
                it.copy(
                    showSetup = false,
                    learnState = learnState,
                    showResult = false,
                    showPause = false
                )
            }

            startTimer()
            showQuestion()
        }
    }

    fun resumeSession() {
        // Simplified: restart the session
        val state = _uiState.value.learnState ?: return
        _uiState.update {
            it.copy(
                showSetup = false,
                learnState = state.copy(isPaused = false),
                showPause = false
            )
        }
        startTimer()
    }

    fun pauseSession() {
        stopTimer()
        _uiState.update {
            it.copy(
                learnState = it.learnState?.copy(isPaused = true),
                showPause = true
            )
        }
    }

    fun continueFromPause() {
        _uiState.update {
            it.copy(
                showPause = false,
                learnState = it.learnState?.copy(isPaused = false)
            )
        }
        startTimer()
    }

    fun saveAndQuit() {
        stopTimer()
        _uiState.update {
            it.copy(
                showPause = false,
                showSetup = true,
                learnState = null
            )
        }
    }

    fun quitSession() {
        val state = _uiState.value.learnState ?: return
        stopTimer()
        val answered = state.remembered + state.forgotten
        if (answered > 0) {
            recordPartialStats(state)
        }
        _uiState.update {
            it.copy(
                showSetup = true,
                learnState = null,
                showResult = false
            )
        }
    }

    // ===== Flashcard =====
    fun flipCard() {
        val state = _uiState.value.learnState ?: return
        if (state.answered) return
        _uiState.update {
            it.copy(learnState = state.copy(answered = true))
        }
    }

    fun answerCard(remembered: Boolean) {
        val state = _uiState.value.learnState ?: return
        if (state.answered && state.mode == "flashcard") return
        if (state.mode != "srs" && state.answered) return

        val card = state.currentCard ?: return

        viewModelScope.launch {
            // Update wrong book / favorites
            if (state.isWrongBook) {
                if (remembered) {
                    repository.removeFromWrongBook(card.word, card.originBank)
                } else {
                    repository.addToWrongBook(card.word, card.definition, card.originBank)
                }
            } else if (!state.isFavorites) {
                if (!remembered) {
                    repository.addToWrongBook(card.word, card.definition, state.bankName)
                }
            }

            val newRemembered = if (remembered) state.remembered + 1 else state.remembered
            val newForgotten = if (!remembered) state.forgotten + 1 else state.forgotten
            val newIndex = state.index + 1

            if (newIndex >= state.cards.size) {
                endSession(state.copy(remembered = newRemembered, forgotten = newForgotten, answered = true))
            } else {
                delay(200)
                _uiState.update {
                    it.copy(
                        learnState = state.copy(
                            index = newIndex,
                            remembered = newRemembered,
                            forgotten = newForgotten,
                            answered = false
                        )
                    )
                }
                showQuestion()
            }
        }
    }

    // ===== SRS =====
    fun flipSrsCard() {
        flipCard()
    }

    fun answerSrsCard(rating: Int) {
        val state = _uiState.value.learnState ?: return
        val card = state.currentCard ?: return

        viewModelScope.launch {
            if (!state.isWrongBook && !state.isFavorites) {
                repository.answerSrsCard(state.bankName, card.word, card.definition, rating)
            }

            val remembered = rating >= 3
            val newRemembered = if (remembered) state.remembered + 1 else state.remembered
            val newForgotten = if (!remembered) state.forgotten + 1 else state.forgotten
            val newIndex = state.index + 1

            _uiState.update {
                it.copy(learnState = state.copy(answered = true))
            }

            if (newIndex >= state.cards.size) {
                delay(300)
                endSession(state.copy(remembered = newRemembered, forgotten = newForgotten))
            } else {
                delay(300)
                _uiState.update {
                    it.copy(
                        learnState = state.copy(
                            index = newIndex,
                            remembered = newRemembered,
                            forgotten = newForgotten,
                            answered = false
                        )
                    )
                }
                showQuestion()
            }
        }
    }

    // ===== Quiz =====
    fun showQuizQuestion() {
        val state = _uiState.value.learnState ?: return
        val card = state.currentCard ?: return

        // Build options
        val correctAnswer = card.back
        val distractors = state.cards
            .filter { it.back != correctAnswer }
            .shuffled()
            .take(3)
            .map { it.back }

        val options = (distractors + correctAnswer).shuffled().map { text ->
            QuizOption(text = text, isCorrect = text == correctAnswer)
        }

        _uiState.update { it.copy(quizOptions = options) }
    }

    fun answerQuiz(selectedOption: QuizOption) {
        val state = _uiState.value.learnState ?: return
        if (state.answered) return

        val isCorrect = selectedOption.isCorrect
        val updatedOptions = _uiState.value.quizOptions.map { opt ->
            opt.copy(
                isDisabled = true,
                isSelected = opt.text == selectedOption.text
            )
        }

        _uiState.update {
            it.copy(
                quizOptions = updatedOptions,
                learnState = state.copy(answered = true)
            )
        }

        viewModelScope.launch {
            val card = state.currentCard ?: return@launch
            if (!state.isWrongBook && !state.isFavorites && !isCorrect) {
                repository.addToWrongBook(card.word, card.definition, state.bankName)
            }

            val newRemembered = if (isCorrect) state.remembered + 1 else state.remembered
            val newForgotten = if (!isCorrect) state.forgotten + 1 else state.forgotten
            val newIndex = state.index + 1

            delay(800)

            if (newIndex >= state.cards.size) {
                endSession(state.copy(remembered = newRemembered, forgotten = newForgotten))
            } else {
                _uiState.update {
                    it.copy(
                        learnState = state.copy(
                            index = newIndex,
                            remembered = newRemembered,
                            forgotten = newForgotten,
                            answered = false
                        ),
                        quizOptions = emptyList()
                    )
                }
                showQuestion()
            }
        }
    }

    // ===== Typing =====
    fun submitTypingAnswer(answer: String) {
        val state = _uiState.value.learnState ?: return
        if (state.answered) return
        val card = state.currentCard ?: return

        val isCorrect = answer.trim().equals(card.word.trim(), ignoreCase = true)
        _uiState.update {
            it.copy(
                typingFeedback = if (isCorrect) "✓ 正确！" else "✗ 正确答案：${card.word}",
                typingIsCorrect = isCorrect,
                learnState = state.copy(answered = true)
            )
        }

        viewModelScope.launch {
            if (!state.isWrongBook && !state.isFavorites && !isCorrect) {
                repository.addToWrongBook(card.word, card.definition, state.bankName)
            }

            val newRemembered = if (isCorrect) state.remembered + 1 else state.remembered
            val newForgotten = if (!isCorrect) state.forgotten + 1 else state.forgotten
            val newIndex = state.index + 1

            if (newIndex >= state.cards.size) {
                endSession(state.copy(remembered = newRemembered, forgotten = newForgotten))
            }
        }
    }

    fun nextTypingQuestion() {
        val state = _uiState.value.learnState ?: return
        if (!state.answered) return
        val newIndex = state.index + 1
        if (newIndex >= state.cards.size) return

        _uiState.update {
            it.copy(
                learnState = state.copy(index = newIndex, answered = false),
                typingFeedback = "",
                typingIsCorrect = null
            )
        }
        showQuestion()
    }

    // ===== Result =====
    private fun endSession(state: LearnState) {
        stopTimer()
        val duration = maxOf((System.currentTimeMillis() - state.startTime) / 1000, 1)
        val accuracy = if (state.totalCards > 0) {
            (state.remembered * 100) / state.totalCards
        } else 0
        val avgTime = duration / maxOf(state.totalCards, 1)

        val modeLabels = mapOf(
            "flashcard" to "学习完成！",
            "quiz" to "答题完成！",
            "typing" to "打字完成！",
            "srs" to "复习完成！"
        )
        val modeNames = mapOf(
            "flashcard" to "闪卡模式",
            "quiz" to "答题模式",
            "typing" to "打字模式",
            "srs" to "间隔重复"
        )

        val result = ResultData(
            bankName = state.bankName,
            mode = state.mode,
            total = state.totalCards,
            remembered = state.remembered,
            forgotten = state.forgotten,
            accuracy = accuracy,
            duration = duration
        )

        _uiState.update {
            it.copy(
                learnState = state,
                showResult = true,
                resultTitle = modeLabels[state.mode] ?: "学习完成！",
                resultBank = "词库：${state.bankName} · ${modeNames[state.mode] ?: "闪卡模式"}",
                resultTotal = state.totalCards,
                resultRemembered = state.remembered,
                resultForgotten = state.forgotten,
                resultAccuracy = "$accuracy%",
                resultDuration = TimeUtils.formatDuration(duration),
                resultAvgTime = "${avgTime}s",
                lastResult = result
            )
        }

        // Save to history
        viewModelScope.launch {
            repository.addHistory(
                HistoryEntity(
                    bankName = state.bankName,
                    mode = state.mode,
                    total = state.totalCards,
                    remembered = state.remembered,
                    forgotten = state.forgotten,
                    accuracy = accuracy,
                    duration = duration
                )
            )
            repository.recordCheckin(state.totalCards)
        }
    }

    private fun recordPartialStats(state: LearnState) {
        viewModelScope.launch {
            val answered = state.remembered + state.forgotten
            if (answered == 0) return@launch
            val duration = (System.currentTimeMillis() - state.startTime) / 1000
            val accuracy = (state.remembered * 100) / answered
            repository.addHistory(
                HistoryEntity(
                    bankName = state.bankName,
                    mode = state.mode,
                    total = answered,
                    remembered = state.remembered,
                    forgotten = state.forgotten,
                    accuracy = accuracy,
                    duration = duration,
                    interrupted = true
                )
            )
            repository.recordCheckin(answered)
        }
    }

    fun goBackToSetup() {
        _uiState.update {
            it.copy(
                showResult = false,
                showSetup = true,
                learnState = null
            )
        }
    }

    fun retrySession() {
        val lastResult = _uiState.value.lastResult ?: return
        val bankName = when {
            lastSessionType == "wrongbook" -> "__wrongbook__"
            lastSessionType == "favorites" -> "__favorites__"
            else -> lastResult.bankName
        }
        _uiState.update { it.copy(showResult = false) }
        startSession(bankName)
    }

    // ===== Timer =====
    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                val state = _uiState.value.learnState ?: break
                val elapsed = (System.currentTimeMillis() - state.startTime) / 1000
                _uiState.update { it.copy(elapsedText = TimeUtils.formatDuration(elapsed)) }
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun showQuestion() {
        val state = _uiState.value.learnState ?: return
        when (state.mode) {
            "quiz" -> showQuizQuestion()
            "typing" -> {
                _uiState.update {
                    it.copy(typingFeedback = "", typingIsCorrect = null)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LearnViewModel(VocabApp.instance.repository) as T
            }
        }
    }
}

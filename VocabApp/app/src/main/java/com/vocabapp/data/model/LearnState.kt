package com.vocabapp.data.model

/**
 * Represents the current state of an active learning session.
 */
data class LearnState(
    val bankName: String,
    val cards: List<Card>,
    val index: Int = 0,
    val remembered: Int = 0,
    val forgotten: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val totalCards: Int = cards.size,
    val answered: Boolean = false,
    val isPaused: Boolean = false,
    val mode: String = "flashcard", // flashcard, quiz, typing, srs
    val direction: String = "word-first", // word-first, def-first
    val isWrongBook: Boolean = false,
    val isFavorites: Boolean = false,
    val elapsedSeconds: Long = 0
) {
    val currentCard: Card?
        get() = if (index < cards.size) cards[index] else null

    val isFinished: Boolean
        get() = index >= cards.size

    val progress: String
        get() = "${index + 1} / $totalCards"
}

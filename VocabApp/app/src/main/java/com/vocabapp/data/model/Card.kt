package com.vocabapp.data.model

/**
 * Represents a learning card used during study sessions.
 */
data class Card(
    val word: String,
    val definition: String,
    val front: String,
    val back: String,
    val originBank: String = ""
)

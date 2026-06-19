package com.vocabapp.data.model

data class BuiltInBank(
    val name: String,
    val icon: String,
    val desc: String,
    val size: String,
    val cards: List<WordPair>
)

data class WordPair(
    val word: String,
    val definition: String
)

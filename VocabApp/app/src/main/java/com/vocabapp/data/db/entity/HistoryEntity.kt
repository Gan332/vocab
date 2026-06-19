package com.vocabapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String,
    val mode: String, // flashcard, quiz, typing, srs
    val total: Int,
    val remembered: Int,
    val forgotten: Int,
    val accuracy: Int,
    val duration: Long, // seconds
    val date: Long = System.currentTimeMillis(),
    val interrupted: Boolean = false
)

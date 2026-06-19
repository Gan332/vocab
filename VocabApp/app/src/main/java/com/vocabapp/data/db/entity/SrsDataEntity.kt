package com.vocabapp.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "srs_data",
    primaryKeys = ["bankName", "word"]
)
data class SrsDataEntity(
    val bankName: String,
    val word: String,
    val definition: String,
    val ef: Double = 2.5,       // Easiness Factor
    val interval: Int = 0,      // days
    val repetitions: Int = 0,
    val nextReview: Long = 0,
    val lastReview: Long = 0
)

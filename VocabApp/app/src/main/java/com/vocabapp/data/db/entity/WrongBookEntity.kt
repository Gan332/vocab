package com.vocabapp.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "wrong_book",
    primaryKeys = ["word", "bankName"]
)
data class WrongBookEntity(
    val word: String,
    val definition: String,
    val bankName: String,
    val wrongCount: Int = 1,
    val lastWrongAt: Long = System.currentTimeMillis()
)

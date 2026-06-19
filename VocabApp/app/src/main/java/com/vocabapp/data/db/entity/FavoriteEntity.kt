package com.vocabapp.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["word", "bankName"]
)
data class FavoriteEntity(
    val word: String,
    val definition: String,
    val bankName: String,
    val createdAt: Long = System.currentTimeMillis()
)

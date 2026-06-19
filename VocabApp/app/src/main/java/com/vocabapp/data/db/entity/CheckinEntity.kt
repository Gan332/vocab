package com.vocabapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkins")
data class CheckinEntity(
    @PrimaryKey val date: String, // "2026-06-19"
    val count: Int = 0
)

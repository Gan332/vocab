package com.vocabapp.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "words",
    primaryKeys = ["bankName", "word"],
    foreignKeys = [
        ForeignKey(
            entity = BankEntity::class,
            parentColumns = ["name"],
            childColumns = ["bankName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bankName")]
)
data class WordEntity(
    val bankName: String,
    val word: String,
    val definition: String
)

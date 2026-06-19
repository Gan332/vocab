package com.vocabapp.data.db.dao

import androidx.room.*
import com.vocabapp.data.db.entity.WrongBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WrongBookDao {
    @Query("SELECT * FROM wrong_book ORDER BY lastWrongAt DESC")
    fun getAllWrongBook(): Flow<List<WrongBookEntity>>

    @Query("SELECT * FROM wrong_book ORDER BY lastWrongAt DESC")
    suspend fun getWrongBookList(): List<WrongBookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWrongBook(entry: WrongBookEntity)

    @Query("DELETE FROM wrong_book WHERE word = :word AND bankName = :bankName")
    suspend fun deleteWrongBook(word: String, bankName: String)

    @Query("DELETE FROM wrong_book")
    suspend fun clearWrongBook()

    @Query("SELECT COUNT(*) FROM wrong_book")
    suspend fun getWrongBookCount(): Int
}

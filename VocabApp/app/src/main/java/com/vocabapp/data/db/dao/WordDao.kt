package com.vocabapp.data.db.dao

import androidx.room.*
import com.vocabapp.data.db.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE bankName = :bankName ORDER BY word ASC")
    fun getWordsByBank(bankName: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE bankName = :bankName ORDER BY word ASC")
    suspend fun getWordsByBankList(bankName: String): List<WordEntity>

    @Query("SELECT * FROM words WHERE bankName = :bankName AND (word LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%')")
    suspend fun searchWords(bankName: String, query: String): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("DELETE FROM words WHERE bankName = :bankName")
    suspend fun deleteWordsByBank(bankName: String)

    @Query("SELECT COUNT(*) FROM words WHERE bankName = :bankName")
    suspend fun getWordCount(bankName: String): Int
}

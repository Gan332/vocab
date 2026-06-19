package com.vocabapp.data.db.dao

import androidx.room.*
import com.vocabapp.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY date DESC LIMIT 200")
    suspend fun getRecentHistory(): List<HistoryEntity>

    @Insert
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun getSessionCount(): Int

    @Query("SELECT COALESCE(SUM(duration), 0) FROM history")
    suspend fun getTotalDuration(): Long

    @Query("SELECT COALESCE(AVG(accuracy), 0) FROM history")
    suspend fun getAverageAccuracy(): Double
}

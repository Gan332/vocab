package com.vocabapp.data.db.dao

import androidx.room.*
import com.vocabapp.data.db.entity.SrsDataEntity

@Dao
interface SrsDataDao {
    @Query("SELECT * FROM srs_data WHERE bankName = :bankName")
    suspend fun getSrsDataByBank(bankName: String): List<SrsDataEntity>

    @Query("SELECT * FROM srs_data WHERE bankName = :bankName AND nextReview <= :now")
    suspend fun getDueCards(bankName: String, now: Long = System.currentTimeMillis()): List<SrsDataEntity>

    @Query("SELECT COUNT(*) FROM srs_data WHERE bankName = :bankName AND nextReview <= :now")
    suspend fun countDueCards(bankName: String, now: Long = System.currentTimeMillis()): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSrsData(data: SrsDataEntity)

    @Query("DELETE FROM srs_data WHERE bankName = :bankName")
    suspend fun deleteSrsDataByBank(bankName: String)
}

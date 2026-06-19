package com.vocabapp.data.db.dao

import androidx.room.*
import com.vocabapp.data.db.entity.CheckinEntity

@Dao
interface CheckinDao {
    @Query("SELECT * FROM checkins WHERE date = :date")
    suspend fun getCheckin(date: String): CheckinEntity?

    @Query("SELECT * FROM checkins")
    suspend fun getAllCheckins(): List<CheckinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckin(checkin: CheckinEntity)

    @Query("UPDATE checkins SET count = count + :delta WHERE date = :date")
    suspend fun incrementCheckin(date: String, delta: Int)

    @Query("SELECT COALESCE(SUM(count), 0) FROM checkins WHERE date = :date")
    suspend fun getTodayCount(date: String): Int
}

package com.vocabapp.data.db.dao

import androidx.room.*
import com.vocabapp.data.db.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    suspend fun getFavoritesList(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE word = :word AND bankName = :bankName")
    suspend fun deleteFavorite(word: String, bankName: String)

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE word = :word AND bankName = :bankName)")
    suspend fun isFavorite(word: String, bankName: String): Boolean

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoriteCount(): Int
}

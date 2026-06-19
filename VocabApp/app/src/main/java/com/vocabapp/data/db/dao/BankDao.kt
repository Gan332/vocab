package com.vocabapp.data.db.dao

import androidx.room.*
import com.vocabapp.data.db.entity.BankEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    @Query("SELECT * FROM banks ORDER BY updatedAt DESC")
    fun getAllBanks(): Flow<List<BankEntity>>

    @Query("SELECT * FROM banks ORDER BY updatedAt DESC")
    suspend fun getAllBanksList(): List<BankEntity>

    @Query("SELECT * FROM banks WHERE name = :name")
    suspend fun getBank(name: String): BankEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: BankEntity)

    @Delete
    suspend fun deleteBank(bank: BankEntity)

    @Query("DELETE FROM banks WHERE name = :name")
    suspend fun deleteBankByName(name: String)

    @Query("SELECT COUNT(*) FROM banks")
    suspend fun getBankCount(): Int
}

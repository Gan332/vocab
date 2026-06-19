package com.vocabapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vocabapp.data.db.dao.*
import com.vocabapp.data.db.entity.*

@Database(
    entities = [
        BankEntity::class,
        WordEntity::class,
        HistoryEntity::class,
        WrongBookEntity::class,
        FavoriteEntity::class,
        SrsDataEntity::class,
        CheckinEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VocabDatabase : RoomDatabase() {
    abstract fun bankDao(): BankDao
    abstract fun wordDao(): WordDao
    abstract fun historyDao(): HistoryDao
    abstract fun wrongBookDao(): WrongBookDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun srsDataDao(): SrsDataDao
    abstract fun checkinDao(): CheckinDao

    companion object {
        @Volatile
        private var INSTANCE: VocabDatabase? = null

        fun getDatabase(context: Context): VocabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VocabDatabase::class.java,
                    "vocab_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.vocabapp

import android.app.Application
import com.vocabapp.data.db.VocabDatabase
import com.vocabapp.data.repository.VocabRepository

class VocabApp : Application() {

    lateinit var repository: VocabRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val db = VocabDatabase.getDatabase(this)
        repository = VocabRepository(
            bankDao = db.bankDao(),
            wordDao = db.wordDao(),
            historyDao = db.historyDao(),
            wrongBookDao = db.wrongBookDao(),
            favoriteDao = db.favoriteDao(),
            srsDataDao = db.srsDataDao(),
            checkinDao = db.checkinDao()
        )
    }

    companion object {
        lateinit var instance: VocabApp
            private set
    }
}

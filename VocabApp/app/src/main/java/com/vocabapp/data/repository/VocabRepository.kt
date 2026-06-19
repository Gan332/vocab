package com.vocabapp.data.repository

import com.vocabapp.data.db.dao.*
import com.vocabapp.data.db.entity.*
import com.vocabapp.data.model.Card
import com.vocabapp.util.SrsAlgorithm
import com.vocabapp.util.TimeUtils
import kotlinx.coroutines.flow.Flow

class VocabRepository(
    private val bankDao: BankDao,
    private val wordDao: WordDao,
    private val historyDao: HistoryDao,
    private val wrongBookDao: WrongBookDao,
    private val favoriteDao: FavoriteDao,
    private val srsDataDao: SrsDataDao,
    private val checkinDao: CheckinDao
) {
    // ===== Banks =====
    fun getAllBanks(): Flow<List<BankEntity>> = bankDao.getAllBanks()

    suspend fun getBank(name: String): BankEntity? = bankDao.getBank(name)

    suspend fun importBank(name: String, words: List<com.vocabapp.data.model.WordPair>) {
        val existing = bankDao.getBank(name)
        if (existing != null) {
            bankDao.insertBank(existing.copy(count = words.size, updatedAt = System.currentTimeMillis()))
            wordDao.deleteWordsByBank(name)
        } else {
            bankDao.insertBank(
                BankEntity(name = name, count = words.size)
            )
        }
        wordDao.insertWords(words.map {
            WordEntity(bankName = name, word = it.word, definition = it.definition)
        })
    }

    suspend fun deleteBank(name: String) {
        bankDao.deleteBankByName(name)
        srsDataDao.deleteSrsDataByBank(name)
    }

    // ===== Words =====
    fun getWordsByBank(bankName: String): Flow<List<WordEntity>> = wordDao.getWordsByBank(bankName)

    suspend fun getWordsByBankList(bankName: String): List<WordEntity> = wordDao.getWordsByBankList(bankName)

    suspend fun searchWords(bankName: String, query: String): List<WordEntity> =
        wordDao.searchWords(bankName, query)

    // ===== History =====
    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun addHistory(history: HistoryEntity) = historyDao.insertHistory(history)

    suspend fun getSessionCount(): Int = historyDao.getSessionCount()

    suspend fun getTotalDuration(): Long = historyDao.getTotalDuration()

    suspend fun getAverageAccuracy(): Double = historyDao.getAverageAccuracy()

    // ===== Wrong Book =====
    fun getAllWrongBook(): Flow<List<WrongBookEntity>> = wrongBookDao.getAllWrongBook()

    suspend fun getWrongBookList(): List<WrongBookEntity> = wrongBookDao.getWrongBookList()

    suspend fun addToWrongBook(word: String, definition: String, bankName: String) {
        val existing = wrongBookDao.getWrongBookList()
        val found = existing.find { it.word == word && it.bankName == bankName }
        if (found != null) {
            wrongBookDao.upsertWrongBook(
                found.copy(wrongCount = found.wrongCount + 1, lastWrongAt = System.currentTimeMillis())
            )
        } else {
            wrongBookDao.upsertWrongBook(
                WrongBookEntity(word = word, definition = definition, bankName = bankName)
            )
        }
    }

    suspend fun removeFromWrongBook(word: String, bankName: String) {
        wrongBookDao.deleteWrongBook(word, bankName)
    }

    suspend fun clearWrongBook() = wrongBookDao.clearWrongBook()

    suspend fun getWrongBookCount(): Int = wrongBookDao.getWrongBookCount()

    // ===== Favorites =====
    fun getAllFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()

    suspend fun getFavoritesList(): List<FavoriteEntity> = favoriteDao.getFavoritesList()

    suspend fun toggleFavorite(word: String, definition: String, bankName: String): Boolean {
        return if (favoriteDao.isFavorite(word, bankName)) {
            favoriteDao.deleteFavorite(word, bankName)
            false
        } else {
            favoriteDao.insertFavorite(FavoriteEntity(word = word, definition = definition, bankName = bankName))
            true
        }
    }

    suspend fun isFavorite(word: String, bankName: String): Boolean =
        favoriteDao.isFavorite(word, bankName)

    suspend fun clearFavorites() = favoriteDao.clearFavorites()

    // ===== SRS =====
    suspend fun getDueCards(bankName: String): List<SrsDataEntity> =
        srsDataDao.getDueCards(bankName)

    suspend fun countDueCards(bankName: String): Int = srsDataDao.countDueCards(bankName)

    suspend fun ensureSrsData(bankName: String, word: String, definition: String) {
        val data = srsDataDao.getSrsDataByBank(bankName)
        if (data.none { it.word == word }) {
            srsDataDao.upsertSrsData(
                SrsAlgorithm.createInitialSrsData(bankName, word, definition)
            )
        }
    }

    suspend fun answerSrsCard(bankName: String, word: String, definition: String, rating: Int) {
        val data = srsDataDao.getSrsDataByBank(bankName)
        val existing = data.find { it.word == word }
        val updated = if (existing != null) {
            SrsAlgorithm.updateSrsData(existing, rating)
        } else {
            SrsAlgorithm.updateSrsData(
                SrsAlgorithm.createInitialSrsData(bankName, word, definition),
                rating
            )
        }
        srsDataDao.upsertSrsData(updated)
    }

    // ===== Check-in =====
    suspend fun recordCheckin(count: Int) {
        val today = TimeUtils.getTodayDateString()
        val existing = checkinDao.getCheckin(today)
        if (existing != null) {
            checkinDao.incrementCheckin(today, count)
        } else {
            checkinDao.upsertCheckin(CheckinEntity(date = today, count = count))
        }
    }

    suspend fun getTodayCount(): Int {
        val today = TimeUtils.getTodayDateString()
        return checkinDao.getTodayCount(today)
    }

    suspend fun getAllCheckins(): List<CheckinEntity> = checkinDao.getAllCheckins()

    // ===== Session =====
    suspend fun saveSession(state: com.vocabapp.data.model.LearnState) {
        // Saved in SharedPreferences or a session table
    }

    // ===== Card Generation =====
    suspend fun createSessionCards(
        bankName: String,
        direction: String,
        mode: String
    ): List<Card> {
        if (mode == "srs") {
            val due = getDueCards(bankName)
            return due.map { c ->
                val isWordFirst = direction == "word-first"
                Card(
                    word = c.word,
                    definition = c.definition,
                    front = if (isWordFirst) c.word else c.definition,
                    back = if (isWordFirst) c.definition else c.word,
                    originBank = bankName
                )
            }
        } else {
            val words = getWordsByBankList(bankName)
            // Ensure SRS data exists for each word
            words.forEach { ensureSrsData(bankName, it.word, it.definition) }
            return words.map { w ->
                val isWordFirst = direction == "word-first"
                Card(
                    word = w.word,
                    definition = w.definition,
                    front = if (isWordFirst) w.word else w.definition,
                    back = if (isWordFirst) w.definition else w.word,
                    originBank = bankName
                )
            }
        }
    }

    fun getWrongBookCards(direction: String): List<Card> {
        val wrongBook = runBlockingSafe { getWrongBookList() } ?: return emptyList()
        return wrongBook.map { w ->
            val isWordFirst = direction == "word-first"
            Card(
                word = w.word,
                definition = w.definition,
                front = if (isWordFirst) w.word else w.definition,
                back = if (isWordFirst) w.definition else w.word,
                originBank = w.bankName
            )
        }
    }

    fun getFavoriteCards(direction: String): List<Card> {
        val favs = runBlockingSafe { getFavoritesList() } ?: return emptyList()
        return favs.map { f ->
            val isWordFirst = direction == "word-first"
            Card(
                word = f.word,
                definition = f.definition,
                front = if (isWordFirst) f.word else f.definition,
                back = if (isWordFirst) f.definition else f.word,
                originBank = f.bankName
            )
        }
    }

    private fun <T> runBlockingSafe(block: suspend () -> T): T? {
        return try {
            kotlinx.coroutines.runBlocking { block() }
        } catch (e: Exception) {
            null
        }
    }
}

package com.vocabapp.util

import com.vocabapp.data.db.entity.SrsDataEntity

/**
 * SM-2 Algorithm implementation for Spaced Repetition System.
 */
object SrsAlgorithm {

    /**
     * Update SRS data based on user rating (0-5).
     * Rating: 0=完全忘记, 1=错误, 2=模糊, 3=困难, 4=顺利, 5=完美
     */
    fun updateSrsData(
        data: SrsDataEntity,
        rating: Int
    ): SrsDataEntity {
        var ef = data.ef
        var interval = data.interval
        var repetitions = data.repetitions

        if (rating < 3) {
            // Reset on poor recall
            repetitions = 0
            interval = 0
        } else {
            // Calculate new interval
            interval = when {
                repetitions == 0 -> 1
                repetitions == 1 -> 6
                else -> Math.round(interval * ef).toInt()
            }
            repetitions++
        }

        // Update easiness factor
        ef += (0.1 - (5 - rating) * (0.08 + (5 - rating) * 0.02))
        if (ef < 1.3) ef = 1.3
        ef = Math.round(ef * 100) / 100.0

        val intervalMs = interval * 86400000L
        val now = System.currentTimeMillis()

        return data.copy(
            ef = ef,
            interval = interval,
            repetitions = repetitions,
            nextReview = now + intervalMs,
            lastReview = now
        )
    }

    /**
     * Create initial SRS data for a word.
     */
    fun createInitialSrsData(
        bankName: String,
        word: String,
        definition: String
    ): SrsDataEntity {
        return SrsDataEntity(
            bankName = bankName,
            word = word,
            definition = definition,
            ef = 2.5,
            interval = 0,
            repetitions = 0,
            nextReview = 0,
            lastReview = 0
        )
    }
}

package com.vocabapp.util

import com.vocabapp.data.model.WordPair

/**
 * Parses TXT content into word-definition pairs.
 * Supports multiple delimiters: tab, " - ", " | ", "|", "：", ":"
 */
object TxtParser {

    fun parse(text: String): List<WordPair> {
        val lines = text.split(Regex("\\r?\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return lines.mapNotNull { line ->
            val parts = when {
                line.contains("\t") -> line.split("\t")
                line.contains(" - ") -> line.split(" - ")
                line.contains(" | ") -> line.split(" | ")
                line.contains("|") -> line.split("|")
                line.contains("：") -> line.split("：")
                line.contains(":") -> line.split(":")
                else -> null
            }

            if (parts != null && parts.size >= 2) {
                WordPair(
                    word = parts[0].trim(),
                    definition = parts.drop(1).joinToString(" ").trim()
                )
            } else null
        }
    }
}

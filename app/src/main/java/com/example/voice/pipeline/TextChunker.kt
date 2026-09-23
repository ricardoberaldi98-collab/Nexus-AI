package com.example.voice.pipeline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TextChunker {
    // Punctuation marks that denote natural speech pauses
    private val sentenceEndRegex = Regex("(?<=[.!?\\n])\\s+")
    private val clauseEndChars = listOf(',', ';', ':', '—')

    /**
     * Transforms an incoming token stream into speech chunks.
     * As soon as a punctuation mark is reached, the accumulated sentence is emitted.
     */
    fun chunkStream(tokenFlow: Flow<String>): Flow<String> = flow {
        val buffer = StringBuilder()

        tokenFlow.collect { token ->
            buffer.append(token)
            val current = buffer.toString()

            // Check for sentence enders
            val match = sentenceEndRegex.find(current)
            if (match != null) {
                val sentence = current.substring(0, match.range.first).trim()
                val remainder = current.substring(match.range.last + 1)
                if (sentence.isNotBlank()) {
                    emit(cleanMarkdownForSpeech(sentence))
                }
                buffer.clear()
                buffer.append(remainder)
            } else if (current.length > 50) {
                // If the sentence is long and has a comma or semicolon, chunk for lower latency
                val lastClauseIdx = current.indexOfLast { it in clauseEndChars }
                if (lastClauseIdx > 25) {
                    val clause = current.substring(0, lastClauseIdx + 1).trim()
                    val remainder = current.substring(lastClauseIdx + 1)
                    if (clause.isNotBlank()) {
                        emit(cleanMarkdownForSpeech(clause))
                    }
                    buffer.clear()
                    buffer.append(remainder)
                }
            }
        }

        // Emit any trailing text remaining in the buffer
        val leftover = buffer.toString().trim()
        if (leftover.isNotBlank()) {
            emit(cleanMarkdownForSpeech(leftover))
        }
    }

    private fun cleanMarkdownForSpeech(raw: String): String {
        return raw
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // bold
            .replace(Regex("\\*(.*?)\\*"), "$1") // italic
            .replace(Regex("`{1,3}(.*?)`{1,3}"), "$1") // code
            .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "") // headings
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // markdown links
            .replace(Regex("[#*_~]"), "")
            .trim()
    }
}

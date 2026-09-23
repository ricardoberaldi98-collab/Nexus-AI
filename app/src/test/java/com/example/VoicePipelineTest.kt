package com.example

import com.example.voice.pipeline.TextChunker
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoicePipelineTest {

    @Test
    fun testTextChunkerSplitsSentences() = runBlocking {
        val chunker = TextChunker()
        val tokens = listOf("Olá, ", "como ", "você ", "está? ", "Espero ", "que ", "bem.")
        val flow = flowOf(*tokens.toTypedArray())

        val chunks = chunker.chunkStream(flow).toList()

        assertTrue("Should have emitted at least 2 chunks", chunks.size >= 2)
        assertEquals("Olá, como você está?", chunks[0])
        assertEquals("Espero que bem.", chunks[1])
    }

    @Test
    fun testTextChunkerCleansMarkdown() = runBlocking {
        val chunker = TextChunker()
        val tokens = listOf("**Nexus** ", "é *rápido*.")
        val flow = flowOf(*tokens.toTypedArray())

        val chunks = chunker.chunkStream(flow).toList()

        assertEquals(1, chunks.size)
        assertEquals("Nexus é rápido.", chunks[0])
    }
}

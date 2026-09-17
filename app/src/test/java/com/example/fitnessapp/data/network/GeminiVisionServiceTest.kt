package com.example.fitnessapp.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiVisionServiceTest {

    private lateinit var service: GeminiVisionService

    @Before
    fun setup() {
        service = GeminiVisionService()
    }

    @Test
    fun testParseAnalysisResult_fromPureJson() {
        val geminiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\n  \"title\": \"2 Phulkas with Dal Tadka\",\n  \"portionDescription\": \"2 rotis, 1 bowl dal\",\n  \"calories\": 350,\n  \"proteinG\": 12.0,\n  \"carbsG\": 55.0,\n  \"fatG\": 8.0,\n  \"fiberG\": 7.0,\n  \"sugarG\": 2.0,\n  \"sodiumMg\": 400.0,\n  \"potassiumMg\": 350.0,\n  \"healthInsights\": \"High in fiber and complex carbohydrates.\"\n}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val result = service.parseAnalysisResult(geminiResponse)
        assertEquals("2 Phulkas with Dal Tadka", result.title)
        assertEquals(350, result.calories)
        assertEquals(12.0f, result.proteinG, 0.01f)
        assertEquals("High in fiber and complex carbohydrates.", result.healthInsights)
    }

    @Test
    fun testParseAnalysisResult_withMarkdownFencesAndPreamble() {
        val geminiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Here is the nutritional breakdown of the meal:\n```json\n{\n  \"title\": \"Paneer Tikka\",\n  \"portionDescription\": \"6 pieces (150g)\",\n  \"calories\": 280,\n  \"proteinG\": 18.0,\n  \"carbsG\": 10.0,\n  \"fatG\": 18.0,\n  \"healthInsights\": \"Rich in protein and calcium.\"\n}\n```\nEnjoy your meal!"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val result = service.parseAnalysisResult(geminiResponse)
        assertEquals("Paneer Tikka", result.title)
        assertEquals(280, result.calories)
        assertEquals(18.0f, result.proteinG, 0.01f)
    }

    @Test
    fun testExtractJsonText_extractsCleanJson() {
        val textWithPreamble = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Based on the image, here is what I found: {\"title\": \"Idli Sambar\", \"calories\": 220} - good choice!"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val extracted = service.extractJsonText(textWithPreamble)
        assertTrue(extracted.startsWith("{"))
        assertTrue(extracted.endsWith("}"))
        assertTrue(extracted.contains("\"title\": \"Idli Sambar\""))
    }

    @Test
    fun testComputeSha256_producesConsistentHash() {
        val data1 = "test image data 123".toByteArray(Charsets.UTF_8)
        val data2 = "test image data 123".toByteArray(Charsets.UTF_8)
        val hash1 = service.computeSha256(data1)
        val hash2 = service.computeSha256(data2)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }

    @Test
    fun testParseAnalysisResult_handlesStringEncodedNumbers() {
        val geminiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\n  \"title\": \"Chole Bhature\",\n  \"portionDescription\": \"2 bhature, 1 bowl chole\",\n  \"calories\": \"550\",\n  \"proteinG\": \"16.5\",\n  \"carbsG\": \"68.0\",\n  \"fatG\": \"22.0\",\n  \"healthInsights\": \"Indulgent festive meal.\"\n}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val result = service.parseAnalysisResult(geminiResponse)
        assertEquals("Chole Bhature", result.title)
        assertEquals(550, result.calories)
        assertEquals(16.5f, result.proteinG, 0.01f)
    }

    @Test
    fun testParseAnalysisResult_malformedJsonReturnsSafeFallbackWithoutCrashing() {
        val malformedResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Sorry, I am unable to format as JSON right now."
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val result = service.parseAnalysisResult(malformedResponse)
        assertTrue(result.title.isNotBlank())
        assertTrue(result.calories > 0)
    }

    @Test
    fun testCandidateModelsPriority() {
        assertEquals("gemini-3.5-flash-lite", GeminiVisionService.CANDIDATE_MODELS.first())
        assertTrue(GeminiVisionService.CANDIDATE_MODELS.contains("gemini-3.5-flash"))
        assertTrue(!GeminiVisionService.CANDIDATE_MODELS.contains("gemini-2.0-flash"))
    }
}

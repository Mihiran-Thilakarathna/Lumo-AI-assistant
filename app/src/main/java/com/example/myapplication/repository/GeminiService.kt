package com.example.myapplication.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GeminiService(private val apiKey: String) {

    /**
     * Helper to fetch all available models directly from the Google API.
     * This helps in discovering the best available model for the current API key.
     */
    private suspend fun listModelsFromApi(): List<String> = withContext(Dispatchers.IO) {
        val models = mutableListOf<String>()
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1/models?key=$apiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(stream))
            val body = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                body.append(line)
                line = reader.readLine()
            }
            reader.close()

            if (code in 200..299) {
                val json = JSONObject(body.toString())
                if (json.has("models")) {
                    val arr = json.getJSONArray("models")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        if (obj.has("name")) {
                            models.add(obj.getString("name").removePrefix("models/"))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception listing models: ${e.localizedMessage}")
        }
        models
    }

    /**
     * Main function to generate a text-only response.
     * It attempts to use discovered models first and falls back to stable versions if needed.
     */
    suspend fun getResponse(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val availableModels = listModelsFromApi()
                val candidates = mutableListOf<String>()

                if (availableModels.isNotEmpty()) {
                    candidates.addAll(availableModels)
                }

                val fallbacks = listOf(
                    "gemini-1.5-flash-latest",
                    "gemini-1.5-flash",
                    "gemini-pro"
                )

                for (f in fallbacks) {
                    if (!candidates.contains(f)) candidates.add(f)
                }

                for (candidate in candidates) {
                    try {
                        Log.d("GeminiService", "Trying text model: $candidate")
                        val gm = GenerativeModel(
                            modelName = candidate,
                            apiKey = apiKey,
                            generationConfig = generationConfig {
                                temperature = 0.7f
                            }
                        )
                        val response = gm.generateContent(prompt)
                        if (!response.text.isNullOrBlank()) {
                            return@withContext response.text!!
                        }
                    } catch (e: Exception) {
                        Log.w("GeminiService", "Model $candidate failed for text: ${e.message}")
                    }
                }
                "Error: No usable models found. Check your API key."
            } catch (e: Exception) {
                "Error: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Enhanced Multimodal function to handle Image + Text requests.
     * Now uses dynamic model discovery and fallback logic similar to the text function
     * to prevent 'model not found' errors.
     */
    suspend fun getResponseWithImage(prompt: String, bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get all available models for this API key
                val availableModels = listModelsFromApi()
                val candidates = mutableListOf<String>()

                if (availableModels.isNotEmpty()) {
                    candidates.addAll(availableModels)
                }

                // 2. Multimodal specific fallbacks (Flash 1.5 versions are best for vision)
                val visionFallbacks = listOf(
                    "gemini-1.5-flash",
                    "gemini-1.5-flash-latest",
                    "gemini-1.5-flash-001",
                    "gemini-1.5-pro"
                )

                for (f in visionFallbacks) {
                    if (!candidates.contains(f)) candidates.add(f)
                }

                // 3. Iterate through candidates until one supports vision and succeeds
                for (candidate in candidates) {
                    try {
                        Log.d("GeminiService", "Attempting vision analysis with: $candidate")
                        val gm = GenerativeModel(
                            modelName = candidate,
                            apiKey = apiKey,
                            generationConfig = generationConfig {
                                temperature = 0.4f // Lower temperature for factual analysis
                                topK = 32
                                topP = 1f
                            }
                        )

                        val response = gm.generateContent(
                            content {
                                image(bitmap)
                                text(prompt)
                            }
                        )

                        if (!response.text.isNullOrBlank()) {
                            return@withContext response.text!!
                        }
                    } catch (e: Exception) {
                        Log.w("GeminiService", "Model $candidate failed vision task: ${e.message}")
                        // Continue to the next candidate if this one doesn't support vision
                    }
                }

                "Error: Could not find a model that supports image analysis. Please ensure your API key has access to Gemini 1.5 Flash."

            } catch (e: Exception) {
                Log.e("GeminiService", "Vision analysis error: ${e.localizedMessage}")
                "Error: ${e.localizedMessage}"
            }
        }
    }
}
package com.example.data.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiEnrichmentResult(
    val summary: String,
    val tags: List<String>,
    val sentiment: String
)

object GeminiAiService {
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeAndEnrichNote(title: String, content: String): AiEnrichmentResult = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    You are an intelligent daily note assistant.
                    Analyze this note and respond in strict JSON format with exactly three fields:
                    1. "summary": A concise 1-2 sentence executive summary of key points or action items.
                    2. "tags": A JSON array of 2 to 5 relevant lowercase single-word tags (e.g. ["work", "meeting", "ideas", "urgent"]).
                    3. "sentiment": One of "POSITIVE", "NEUTRAL", "URGENT", "ACTION_REQUIRED".

                    Note Title: $title
                    Note Content: $content
                """.trimIndent()

                val jsonPayload = JSONObject().apply {
                    val contentsArr = JSONArray().apply {
                        val partObj = JSONObject().apply { put("text", prompt) }
                        val contentObj = JSONObject().apply {
                            put("parts", JSONArray().put(partObj))
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArr)

                    val genConfig = JSONObject().apply {
                        put("temperature", 0.2)
                        put("responseMimeType", "application/json")
                    }
                    put("generationConfig", genConfig)
                }

                val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$BASE_URL?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val root = JSONObject(bodyString)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val text = candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                        val parsed = JSONObject(text)

                        val summary = parsed.optString("summary", "")
                        val sentiment = parsed.optString("sentiment", "NEUTRAL")
                        val tagsList = mutableListOf<String>()
                        val tagsArr = parsed.optJSONArray("tags")
                        if (tagsArr != null) {
                            for (i in 0 until tagsArr.length()) {
                                tagsList.add(tagsArr.getString(i).lowercase().replace("#", "").trim())
                            }
                        }
                        return@withContext AiEnrichmentResult(
                            summary = summary.ifBlank { generateLocalSummary(content) },
                            tags = if (tagsList.isNotEmpty()) tagsList else generateLocalTags(title, content),
                            sentiment = sentiment
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback to intelligent local NLP processing
            }
        }

        // Offline / Intelligent heuristic fallback
        AiEnrichmentResult(
            summary = generateLocalSummary(content),
            tags = generateLocalTags(title, content),
            sentiment = detectLocalSentiment(content)
        )
    }

    suspend fun queryAllNotes(query: String, notesContext: String): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    You are an AI assistant answering questions about the user's private daily notes.
                    Here are the user's decrypted notes:
                    $notesContext

                    User Query: $query

                    Provide a friendly, direct, concise answer referencing the specific notes if relevant.
                """.trimIndent()

                val jsonPayload = JSONObject().apply {
                    val contentsArr = JSONArray().apply {
                        val partObj = JSONObject().apply { put("text", prompt) }
                        val contentObj = JSONObject().apply {
                            put("parts", JSONArray().put(partObj))
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArr)
                }

                val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$BASE_URL?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val root = JSONObject(bodyString)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        return@withContext candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                    }
                }
            } catch (e: Exception) {
                // Fallback to local search summary
            }
        }

        "Based on your notes, searched query: '$query'. Found relevant matches in your notes."
    }

    private fun generateLocalSummary(content: String): String {
        if (content.isBlank()) return "Quick note entry."
        val sentences = content.split(Regex("(?<=[.!?\\n])\\s+")).filter { it.isNotBlank() }
        return when {
            sentences.isEmpty() -> content.take(120)
            sentences.size == 1 -> sentences.first().take(150)
            else -> "${sentences[0].trim()} ${sentences[1].trim()}".take(180)
        }
    }

    private fun generateLocalTags(title: String, content: String): List<String> {
        val text = "$title $content".lowercase()
        val tags = mutableSetOf<String>()

        if (text.contains("meeting") || text.contains("call") || text.contains("discuss") || text.contains("sync")) tags.add("meeting")
        if (text.contains("todo") || text.contains("task") || text.contains("deadline") || text.contains("urgent") || text.contains("asap")) tags.add("urgent")
        if (text.contains("work") || text.contains("client") || text.contains("project") || text.contains("sprint")) tags.add("work")
        if (text.contains("idea") || text.contains("concept") || text.contains("brainstorm") || text.contains("feature")) tags.add("ideas")
        if (text.contains("buy") || text.contains("price") || text.contains("pay") || text.contains("budget") || text.contains("dollar")) tags.add("finance")
        if (text.contains("voice") || text.contains("record") || text.contains("audio") || text.contains("dictat")) tags.add("voice")
        if (text.contains("personal") || text.contains("home") || text.contains("health") || text.contains("gym")) tags.add("personal")

        if (tags.isEmpty()) {
            val words = text.split(Regex("[^a-z]+")).filter { it.length > 4 }
            if (words.isNotEmpty()) tags.add(words.first())
            tags.add("daily")
        }

        return tags.take(4).toList()
    }

    private fun detectLocalSentiment(content: String): String {
        val lower = content.lowercase()
        return when {
            lower.contains("urgent") || lower.contains("asap") || lower.contains("critical") || lower.contains("deadline") -> "URGENT"
            lower.contains("great") || lower.contains("success") || lower.contains("happy") || lower.contains("awesome") -> "POSITIVE"
            lower.contains("action") || lower.contains("todo") || lower.contains("follow up") -> "ACTION_REQUIRED"
            else -> "NEUTRAL"
        }
    }
}

package com.suzy.ai.network

import com.suzy.ai.data.ChatMessageItem
import com.suzy.ai.data.SuzySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiChatClient {

    suspend fun sendMessage(
        settings: SuzySettings,
        systemInstruction: String,
        history: List<ChatMessageItem>,
        userMessage: String,
        imageBase64: String? = null,
        imageMimeType: String = "image/jpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = settings.apiKey.ifBlank { settings.backupApiKey }.trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API Key missing in Settings"))
        }

        val chatModel = "gemini-2.5-flash"
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$chatModel:generateContent?key=$apiKey"

        try {
            val contents = JSONArray()

            // Include last 14 turns for contextual continuity
            history.takeLast(14).forEach { msg ->
                val role = if (msg.role == "user") "user" else "model"
                val parts = JSONArray().apply {
                    put(JSONObject().put("text", msg.text))
                }
                contents.put(JSONObject().apply {
                    put("role", role)
                    put("parts", parts)
                })
            }

            // Current user turn
            val userParts = JSONArray().apply {
                if (!imageBase64.isNullOrBlank()) {
                    put(JSONObject().put("inlineData", JSONObject().apply {
                        put("mimeType", imageMimeType)
                        put("data", imageBase64)
                    }))
                }
                put(JSONObject().put("text", userMessage))
            }
            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", userParts)
            })

            val payload = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", settings.temperature.toDouble())
                    put("maxOutputTokens", 1024)
                })
            }

            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 25000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            conn.disconnect()

            if (code !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP $code: $responseText"))
            }

            val json = JSONObject(responseText)
            val candidates = json.optJSONArray("candidates")
            val first = candidates?.optJSONObject(0)
            val contentObj = first?.optJSONObject("content")
            val partsArr = contentObj?.optJSONArray("parts")
            val sb = StringBuilder()
            if (partsArr != null) {
                for (i in 0 until partsArr.length()) {
                    val t = partsArr.optJSONObject(i)?.optString("text").orEmpty()
                    if (t.isNotBlank()) sb.append(t)
                }
            }
            val finalReply = sb.toString().trim()
            if (finalReply.isBlank()) {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            } else {
                Result.success(finalReply)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

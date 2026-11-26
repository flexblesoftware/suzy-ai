package com.suzy.ai.network

import android.util.Base64
import com.suzy.ai.data.SuzySettings
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(
    private val onConnected: () -> Unit,
    private val onAudioOutput: (ByteArray) -> Unit,
    private val onTextOutput: (String) -> Unit,
    private val onToolCall: (callId: String, name: String, args: JSONObject) -> Unit,
    private val onInterrupted: () -> Unit,
    private val onTurnComplete: () -> Unit,
    private val onError: (String) -> Unit,
    private val onClosed: () -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    fun connect(settings: SuzySettings) {
        disconnect()

        val wsUrl = "wss://generativelanguage.googleapis.com/ws/" +
            "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent" +
            "?key=${settings.apiKey}"

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                sendSetupMessage(webSocket, settings)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingJson(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleIncomingJson(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val code = response?.code?.let { "HTTP $it: " } ?: ""
                val msg = response?.message?.takeIf { it.isNotBlank() } ?: t.localizedMessage ?: "Connection failed"
                onError("$code$msg")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (code != 1000 && reason.isNotBlank()) {
                    onError("Closed ($code): $reason")
                } else {
                    onClosed()
                }
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket, settings: SuzySettings) {
        val modelName = if (settings.modelId.startsWith("models/")) {
            settings.modelId
        } else {
            "models/${settings.modelId}"
        }

        val setupJson = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", modelName)
                put("generationConfig", JSONObject().apply {
                    put("temperature", settings.temperature.toDouble())
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", settings.voiceName)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", settings.systemPrompt)
                    }))
                })
            })
        }
        ws.send(setupJson.toString())
    }

    fun sendPcmAudio(pcmBytes: ByteArray) {
        val ws = webSocket ?: return
        val b64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
        val payload = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().put(JSONObject().apply {
                    put("mimeType", "audio/pcm;rate=16000")
                    put("data", b64)
                }))
            })
        }
        ws.send(payload.toString())
    }

    fun sendReactionPrompt(promptText: String) {
        val ws = webSocket ?: return
        val payload = JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("turns", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", promptText)
                    }))
                }))
                put("turnComplete", true)
            })
        }
        ws.send(payload.toString())
    }

    fun sendToolResponse(callId: String, name: String, resultText: String) {
        val ws = webSocket ?: return
        val payload = JSONObject().apply {
            put("toolResponse", JSONObject().apply {
                put("functionResponses", JSONArray().put(JSONObject().apply {
                    put("id", callId)
                    put("name", name)
                    put("response", JSONObject().apply {
                        put("result", resultText)
                    })
                }))
            })
        }
        ws.send(payload.toString())
    }

    private fun handleIncomingJson(raw: String) {
        try {
            val json = JSONObject(raw)

            if (json.has("setupComplete")) {
                onConnected()
                return
            }

            val serverContent = json.optJSONObject("serverContent") ?: return

            if (serverContent.optBoolean("interrupted", false)) {
                onInterrupted()
                return
            }

            val modelTurn = serverContent.optJSONObject("modelTurn")
            if (modelTurn != null) {
                val parts = modelTurn.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.optJSONObject(i) ?: continue
                        val text = part.optString("text", "")
                        if (text.isNotBlank()) {
                            onTextOutput(text)
                        }
                        val inlineData = part.optJSONObject("inlineData")
                        if (inlineData != null) {
                            val mime = inlineData.optString("mimeType", "")
                            val data = inlineData.optString("data", "")
                            if (mime.startsWith("audio/") && data.isNotBlank()) {
                                val pcm = Base64.decode(data, Base64.DEFAULT)
                                onAudioOutput(pcm)
                            }
                        }
                    }
                }
            }

            if (serverContent.optBoolean("turnComplete", false)) {
                onTurnComplete()
            }
        } catch (e: Exception) {
            onError("Parse error: ${e.localizedMessage}")
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "User stopped session")
        } catch (_: Exception) {
        }
        webSocket = null
    }
}

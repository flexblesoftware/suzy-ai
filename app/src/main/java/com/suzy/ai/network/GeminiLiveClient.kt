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
    private val onUserSpeechOutput: (String) -> Unit = {},
    private val onToolCall: (callId: String, name: String, args: JSONObject) -> Unit,
    private val onInterrupted: () -> Unit,
    private val onTurnComplete: () -> Unit,
    private val onError: (String) -> Unit,
    private val onClosed: () -> Unit
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var setupCompleted = false

    fun connect(settings: SuzySettings) {
        disconnect()
        setupCompleted = false

        val apiKey = settings.apiKey.trim()
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                sendSetupPayload(webSocket, settings)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerJson(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleServerJson(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val msg = t.message ?: response?.message ?: "WebSocket connection failed"
                onError(msg)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onClosed()
            }
        })
    }

    private fun sendSetupPayload(ws: WebSocket, settings: SuzySettings) {
        val modelName = if (settings.modelId.startsWith("models/")) settings.modelId else "models/${settings.modelId}"

        val strictExecutionDirective = "\n\n[CRITICAL DEVICE & APP COMMAND EXECUTION PROTOCOL]:\n" +
            "- Tumhare paas real Android device tools hain (control_flashlight, control_connectivity, control_dnd_mode, control_volume_brightness, accessibility_action, execute_omni_command).\n" +
            "- Jab bhi user flashlight/torch, mobile data, wifi, hotspot, bluetooth, DND mode, silent mode, brightness, volume, lock screen, screenshot, scroll, timer, persona switch, ya ORB/UI change bole, toh PEHLE uska matching function tool call zaroor execute karo!\n" +
            "- Bina tool call kiye kabhi jhootha 'haan kar diya' mat bolo. Tool ka result milne ke baad 1 chhoti natural Hinglish line mein confirm karo."

        val setupJson = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", modelName)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("temperature", settings.temperature.toDouble())
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", settings.voiceName)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", settings.systemPrompt + strictExecutionDirective)))
                })
                put("inputAudioTranscription", JSONObject())
                put("outputAudioTranscription", JSONObject())
                put("tools", JSONArray().put(JSONObject().apply {
                    put("functionDeclarations", buildToolDeclarations())
                }))
            })
        }

        ws.send(setupJson.toString())
    }

    private fun buildToolDeclarations(): JSONArray {
        return JSONArray().apply {
            // 1. Flashlight / Torch
            put(JSONObject().apply {
                put("name", "control_flashlight")
                put("description", "Turn the phone camera flashlight / torch ON or OFF.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("enable", JSONObject().apply {
                            put("type", "BOOLEAN")
                            put("description", "true to turn flashlight ON, false to turn OFF")
                        })
                    })
                    put("required", JSONArray().put("enable"))
                })
            })

            // 2. Connectivity (Mobile Data, Wi-Fi, Hotspot, Bluetooth, Airplane, Location)
            put(JSONObject().apply {
                put("name", "control_connectivity")
                put("description", "Toggle or open mobile data, wifi, personal hotspot, bluetooth, airplane mode, or GPS location.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("target", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "One of: data, wifi, hotspot, bluetooth, airplane, location")
                        })
                        put("state", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "on, off, or toggle")
                        })
                    })
                    put("required", JSONArray().put("target"))
                })
            })

            // 3. Do Not Disturb (DND) Mode
            put(JSONObject().apply {
                put("name", "control_dnd_mode")
                put("description", "Enable or disable Do Not Disturb (DND) silent focus mode on the phone.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("enable", JSONObject().apply {
                            put("type", "BOOLEAN")
                            put("description", "true to enable DND, false to turn DND off")
                        })
                        put("level", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "PRIORITY or TOTAL")
                        })
                    })
                    put("required", JSONArray().put("enable"))
                })
            })

            // 4. Brightness, Volume & Auto-Rotate
            put(JSONObject().apply {
                put("name", "control_volume_brightness")
                put("description", "Set screen brightness percentage, media/alarm/ring volume percentage, or auto-rotate.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("setting", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "brightness, media, alarm, ring, or rotate")
                        })
                        put("percent", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Percentage from 0 to 100")
                        })
                    })
                    put("required", JSONArray().put("setting").put("percent"))
                })
            })

            // 5. Accessibility Hardware & Screen Gestures
            put(JSONObject().apply {
                put("name", "accessibility_action")
                put("description", "Perform global Android accessibility action: lock_screen, screenshot, power_menu, scroll_down, scroll_up, home, back, recents, quick_settings, or click_text.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("action", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Action name: lock_screen, screenshot, power_menu, scroll_down, scroll_up, home, back, recents, quick_settings, click_text")
                        })
                        put("target_text", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Button text on screen if action is click_text")
                        })
                    })
                    put("required", JSONArray().put("action"))
                })
            })

            // 6. Master Omni-Command Router (Persona switch, ORB morph, Theme skin, Timer, Memory, Udhaar, Navigation, Boss Briefing)
            put(JSONObject().apply {
                put("name", "execute_omni_command")
                put("description", "Execute any Suzy AI app command such as switching persona (Vikram, Suzy, Zara, Tara), morphing 3D ORB (arc reactor, quantum, eclipse), changing theme (amoled, cyberpunk, zen mode), setting focus timer, saving memory/udhaar/task, or opening any screen.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("command", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Natural command instruction to execute in the app")
                        })
                    })
                    put("required", JSONArray().put("command"))
                })
            })

            // 7. Battery Status
            put(JSONObject().apply {
                put("name", "get_battery_status")
                put("description", "Check phone battery percentage, charging status, and temperature.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject())
                })
            })
        }
    }

    private fun handleServerJson(raw: String) {
        try {
            val root = JSONObject(raw)

            if (root.has("setupComplete") && !setupCompleted) {
                setupCompleted = true
                onConnected()
                return
            }

            // Handle Function / Tool Calls from Gemini Live
            val toolCall = root.optJSONObject("toolCall")
            if (toolCall != null) {
                val calls = toolCall.optJSONArray("functionCalls")
                if (calls != null) {
                    for (i in 0 until calls.length()) {
                        val call = calls.optJSONObject(i) ?: continue
                        val id = call.optString("id")
                        val name = call.optString("name")
                        val args = call.optJSONObject("args") ?: JSONObject()
                        if (name.isNotBlank()) {
                            onToolCall(id, name, args)
                        }
                    }
                }
            }

            val serverContent = root.optJSONObject("serverContent") ?: return

            if (serverContent.optBoolean("interrupted", false)) {
                onInterrupted()
            }

            // User Speech Transcription (real-time voice command fallback interceptor!)
            val inputTx = serverContent.optJSONObject("inputTranscription")
            val userSpoken = inputTx?.optString("text").orEmpty()
            if (userSpoken.isNotBlank()) {
                onUserSpeechOutput(userSpoken)
            }

            // AI Speech Transcription
            val outputTx = serverContent.optJSONObject("outputTranscription")
            val aiCaption = outputTx?.optString("text").orEmpty()
            if (aiCaption.isNotBlank()) {
                onTextOutput(aiCaption)
            }

            // Audio & Text Model Turn Parts
            val modelTurn = serverContent.optJSONObject("modelTurn")
            val parts = modelTurn?.optJSONArray("parts")
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val b64 = inlineData.optString("data")
                        if (b64.isNotBlank()) {
                            val pcm = Base64.decode(b64, Base64.DEFAULT)
                            onAudioOutput(pcm)
                        }
                    }
                    val txt = part.optString("text")
                    if (txt.isNotBlank() && aiCaption.isBlank()) {
                        onTextOutput(txt)
                    }
                }
            }

            if (serverContent.optBoolean("turnComplete", false)) {
                onTurnComplete()
            }
        } catch (_: Exception) {
        }
    }

    fun sendPcmAudio(pcmBytes: ByteArray) {
        val ws = webSocket ?: return
        if (!setupCompleted || pcmBytes.isEmpty()) return
        val b64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
        val msg = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().put(JSONObject().apply {
                    put("mimeType", "audio/pcm;rate=16000")
                    put("data", b64)
                }))
            })
        }
        ws.send(msg.toString())
    }

    fun sendReactionPrompt(prompt: String) {
        val ws = webSocket ?: return
        if (!setupCompleted) return
        val msg = JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("turns", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }))
                put("turnComplete", true)
            })
        }
        ws.send(msg.toString())
    }

    fun sendToolResponse(callId: String, name: String, result: String) {
        val ws = webSocket ?: return
        val msg = JSONObject().apply {
            put("toolResponse", JSONObject().apply {
                put("functionResponses", JSONArray().put(JSONObject().apply {
                    put("id", callId)
                    put("name", name)
                    put("response", JSONObject().put("result", result))
                }))
            })
        }
        ws.send(msg.toString())
    }

    fun disconnect() {
        setupCompleted = false
        try {
            webSocket?.close(1000, "Session closed")
        } catch (_: Exception) {
        }
        webSocket = null
    }
}

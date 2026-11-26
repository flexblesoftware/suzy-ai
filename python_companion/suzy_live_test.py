"""
Suzy Gemini Live API WebSocket Tester (Python)
Termux ya PC se direct apna API key aur Model ID test karne ke liye.
"""
import asyncio
import json
import os
import websockets

API_KEY = os.getenv("GEMINI_API_KEY", "")
MODEL_ID = os.getenv("SUZY_MODEL_ID", "models/gemini-2.5-flash-native-audio-preview-12-2025")
VOICE_NAME = os.getenv("SUZY_VOICE", "Aoede")

async def test_suzy_live():
    if not API_KEY:
        print("Error: GEMINI_API_KEY environment variable set karo.")
        return

    uri = (
        "wss://generativelanguage.googleapis.com/ws/"
        "google.ai.generativelanguage.v1beta.BidiGenerateContent/BidiGenerateContent"
        f"?key={API_KEY}"
    )

    async with websockets.connect(uri) as ws:
        setup_msg = {
            "setup": {
                "model": MODEL_ID,
                "generationConfig": {
                    "responseModalities": ["AUDIO"],
                    "speechConfig": {
                        "voiceConfig": {
                            "prebuiltVoiceConfig": {"voiceName": VOICE_NAME}
                        }
                    }
                },
                "systemInstruction": {
                    "parts": [{
                        "text": "Tumhara naam Suzy hai. Hinglish mein pyaar se baat karo."
                    }]
                }
            }
        }
        await ws.send(json.dumps(setup_msg))
        raw = await ws.recv()
        print("Server response:", raw[:300])
        print("Suzy Live WebSocket connection 100% working hai!")

if __name__ == "__main__":
    asyncio.run(test_suzy_live())

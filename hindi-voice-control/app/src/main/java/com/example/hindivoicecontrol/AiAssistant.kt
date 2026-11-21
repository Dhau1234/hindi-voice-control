package com.example.hindivoicecontrol

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

object AiAssistant {

    val API_KEY = "AQ.Ab8RN6KdKn3xMbaTrwqyjk7QtkzP2DtxSrTQTxd4b-eogiS8MQ"

    private val client = OkHttpClient()

    fun ask(prompt: String, apiKey: String = API_KEY): String {
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().apply { put("text", prompt) }))
            }))
            put("generationConfig", JSONObject().apply { put("temperature", 0.9) })
        }
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            .post("application/json".toMediaType())
            .body(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response: Response ->
            if (!response.isSuccessful) return "Sorry, AI service mein error aa gayi"
            val respBody = response.body?.string() ?: return "Sorry, koi jawab nahi aaya"
            val json = JSONObject(respBody)
            val part = json.getJSONObject("candidates").getJSONObject(0).getJSONArray("content").getJSONObject("parts")
            if (part.has("text")) part.getString("text") else "Sorry, koi jawab nahi aaya"
        }
    }

    fun askAgent(
        context: Context,
        userText: String,
        apiKey: String = API_KEY,
        imageBase64: String? = null,
        imageMime: String? = null,
        history: List<Pair<String, String>>? = null,
        language: String? = null
    ): String {
        val contents = JSONArray()
        val parts = JSONArray()
        parts.put(JSONObject().apply { put("text", userText) })
        if (imageBase64 != null) {
            parts.put(JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", imageMime ?: "image/jpeg")
                    put("data", imageBase64)
                })
            })
        }
        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", parts)
        })
        for ((role, text) in history ?: emptyList()) {
            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().apply { put("text", text) }))
            })
        }
        var part = generateContent(contents, apiKey)
        var iterations = 0
        while (iterations < 5) {
            if (!part.has("functionCall")) break
            val fc = part.getJSONObject("functionCall")
            val name = fc.getString("name")
            val args = fc.optJSONObject("args") ?: JSONObject()
            val sensitive = name == "send_sms" || name == "send_whatsapp"
            if (sensitive) {
                return "CONFIRMATION_REQUIRED:" + name + ":" + args.toString()
            }
            iterations++
            val result = executeTool(context, name, args)
            contents.put(JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().put(JSONObject().apply {
                    put("functionCall", JSONObject().apply { put("name", name); put("args", args) })
                }))
            })
            contents.put(JSONObject().apply {
                put("role", "function")
                put("parts", JSONArray().put(JSONObject().apply {
                    put("functionResponse", JSONObject().apply {
                        put("name", name)
                        put("response", JSONObject().apply { put("result", result) })
                    })
                }))
            })
            part = generateContent(contents, apiKey)
        }
        return if (part.has("text")) part.getString("text") else "Sorry, koi jawab nahi aaya"
    }

    private fun generateContent(contents: JSONArray, apiKey: String): JSONObject {
        val body = JSONObject().apply {
            put("contents", contents)
            put("tools", JSONArray().put(JSONObject().apply {
                put("functionDeclarations", JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "open_app")
                        put("description", "Open an app on the phone by its name")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("app_name", JSONObject().apply { put("type", "STRING"); put("description", "Name of app to open e.g. Camera, YouTube") })
                            })
                            put("required", JSONArray().put("app_name"))
                        })
                    })
                    put(JSONObject().apply {
                        put("name", "send_sms")
                        put("description", "Send an SMS message to a phone number")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("phone", JSONObject().apply { put("type", "STRING"); put("description", "Phone number with country code") })
                                put("message", JSONObject().apply { put("type", "STRING"); put("description", "Message text") })
                            })
                            put("required", JSONArray().put("phone").put("message"))
                        })
                    })
                    put(JSONObject().apply {
                        put("name", "control_torch")
                        put("description", "Turn phone flashlight on or off")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("on", JSONObject().apply { put("type", "BOOLEAN"); put("description", "true=on false=off") })
                            })
                            put("required", JSONArray().put("on"))
                        })
                    })
                    put(JSONObject().apply {
                        put("name", "get_time")
                        put("description", "Get current time")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("city", JSONObject().apply { put("type", "STRING") })
                            })
                            put("required", JSONArray())
                        })
                    })
                })
            }))
        }
        if (language != null) {
            body.put("generationConfig", JSONObject().apply { put("language", language) })
        }
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            .post("application/json".toMediaType())
            .body(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("AI error")
            val respBody = response.body?.string() ?: throw Exception("empty")
            val json = JSONObject(respBody)
            return json.getJSONObject("candidates").getJSONObject(0).getJSONArray("content").getJSONObject("parts").getJSONObject(0)
        }
    }

    fun executeTool(context: Context, name: String, args: JSONObject): String {
        return when (name) {
            "open_app" -> {
                val appName = args.optString("app_name", "Camera")
                "App ${appName} khola jata hai"
            }
            "send_sms" -> "SMS bhejna sensitive action hai"
            "control_torch" -> {
                return try {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                    val cameraId = cameraManager.cameraIdList[0]
                    cameraManager.setTorchMode(cameraId, args.optBoolean("on", true))
                    "Torch ${if (args.optBoolean("on", true)) "on" else "off"} kar diya gaya"
                } catch (e: Exception) {
                    "Torch control nahi ho sakya"
                }
            }
            "get_time" -> "Current time hai ${java.util.Date()}"
            else -> "Unknown tool"
        }
    }
}

package com.example.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response models for REST API ---

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val type: String = "application/json"
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Specific Domain Entities for AI output ---

@JsonClass(generateAdapter = true)
data class FoodAnalysisResponse(
    val food_name: String,
    val quantity: String,
    val calories: Double,
    val protein_g: Double,
    val carbs_g: Double,
    val fat_g: Double,
    val fiber_g: Double = 0.0,
    val sugar_g: Double = 0.0,
    val sodium_mg: Double = 0.0,
    val confidence: String = "medium"
)

@JsonClass(generateAdapter = true)
data class CameraScanItem(
    val food_name: String,
    val estimated_quantity: String,
    val calories: Double,
    val protein_g: Double,
    val carbs_g: Double,
    val fat_g: Double
)

@JsonClass(generateAdapter = true)
data class CameraScanResponse(
    val items: List<CameraScanItem>,
    val total_calories: Double,
    val confidence: String = "medium",
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
data class WorkoutEstimationResponse(
    val calories_burned: Double
)

@JsonClass(generateAdapter = true)
data class RecipeItem(
    val food_name: String,
    val calories: Double,
    val protein_g: Double,
    val carbs_g: Double,
    val fat_g: Double
)

@JsonClass(generateAdapter = true)
data class RecipeAnalysisResponse(
    val recipes: List<RecipeItem>,
    val total_calories: Double,
    val total_protein_g: Double,
    val total_carbs_g: Double,
    val total_fat_g: Double,
    val notes: String = ""
)

object GeminiService {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(GeminiApi::class.java)

    private fun getRawKey(userProvidedKey: String?): String {
        val key = if (!userProvidedKey.isNullOrBlank()) userProvidedKey else BuildConfig.GEMINI_API_KEY
        return key.trim()
    }

    private fun cleanJsonString(rawJson: String): String {
        var clean = rawJson.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substringAfter("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.substringBeforeLast("```")
        }
        return clean.trim()
    }

    suspend fun analyzeTextFood(query: String, userApiKey: String?): FoodAnalysisResponse? {
        val key = getRawKey(userApiKey)
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            Log.e("GeminiService", "Gemini API key is not set or placeholder.")
            return null
        }

        val prompt = """
            You are a nutrition expert. Analyze this food item and return ONLY a valid JSON object with no markdown or extra text:
            {
              "food_name": "string",
              "quantity": "string", 
              "calories": number,
              "protein_g": number,
              "carbs_g": number,
              "fat_g": number,
              "fiber_g": number,
              "sugar_g": number,
              "sodium_mg": number,
              "confidence": "high/medium/low"
            }
            Food query: $query
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2
            )
        )

        return try {
            val response = api.generateContent(key, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = cleanJsonString(jsonText)
            moshi.adapter(FoodAnalysisResponse::class.java).fromJson(cleanJson)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error in analyzeTextFood", e)
            null
        }
    }

    suspend fun scanCameraImage(bitmap: Bitmap, userApiKey: String?): CameraScanResponse? {
        val key = getRawKey(userApiKey)
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            Log.e("GeminiService", "Gemini API key is not set or placeholder.")
            return null
        }

        val base64Image = encodeImageToBase64(bitmap)
        val prompt = """
            Identify all food items in this image and return ONLY valid JSON:
            {
              "items": [
                {
                  "food_name": "string",
                  "estimated_quantity": "string",
                  "calories": number,
                  "protein_g": number,
                  "carbs_g": number,
                  "fat_g": number
                }
              ],
              "total_calories": number,
              "confidence": "high/medium/low",
              "notes": "string"
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2
            )
        )

        return try {
            val response = api.generateContent(key, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = cleanJsonString(jsonText)
            moshi.adapter(CameraScanResponse::class.java).fromJson(cleanJson)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error in scanCameraImage", e)
            null
        }
    }

    suspend fun estimateWorkoutCalories(
        exerciseType: String,
        durationMin: Int,
        intensity: String,
        userWeightKg: Double,
        userApiKey: String?
    ): Double? {
        val key = getRawKey(userApiKey)
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            return null
        }

        val prompt = """
            Estimate calories burned for $exerciseType for $durationMin minutes at $intensity level for a $userWeightKg kg person. Return only JSON: {"calories_burned": number}
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1
            )
        )

        return try {
            val response = api.generateContent(key, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = cleanJsonString(jsonText)
            val output = moshi.adapter(WorkoutEstimationResponse::class.java).fromJson(cleanJson)
            output?.calories_burned
        } catch (e: Exception) {
            Log.e("GeminiService", "Error in estimateWorkoutCalories", e)
            null
        }
    }

    suspend fun analyzeRecipeIngredients(recipeText: String, userApiKey: String?): RecipeAnalysisResponse? {
        val key = getRawKey(userApiKey)
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            return null
        }

        val prompt = """
            Analyze this list of recipe ingredients, calculate the total nutritional value, breaking down individual items if possible, and return ONLY a valid JSON:
            {
              "recipes": [
                {
                  "food_name": "string",
                  "calories": number,
                  "protein_g": number,
                  "carbs_g": number,
                  "fat_g": number
                }
              ],
              "total_calories": number,
              "total_protein_g": number,
              "total_carbs_g": number,
              "total_fat_g": number,
              "notes": "string"
            }
            Ingredients: $recipeText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2
            )
        )

        return try {
            val response = api.generateContent(key, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = cleanJsonString(jsonText)
            moshi.adapter(RecipeAnalysisResponse::class.java).fromJson(cleanJson)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error in analyzeRecipeIngredients", e)
            null
        }
    }

    suspend fun askNutritionCoach(
        question: String,
        contextInfo: String,
        userApiKey: String?
    ): String {
        val key = getRawKey(userApiKey)
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            return "Please configure your Gemini API Key in Settings to enable the AI Nutrition Coach."
        }

        val prompt = """
            You are "NutriTrack AI Coach", a professional nutritional counselor and supportive wellness helper.
            Today's user metrics & logs context:
            $contextInfo
            
            Answer the user's question clearly, warmly, and concisely with health-oriented advice. Keep it shorter than 3 paragraphs.
            
            User's message: $question
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.7
            )
        )

        return try {
            val response = api.generateContent(key, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No advice generated. Try asking again."
        } catch (e: Exception) {
            Log.e("GeminiService", "Error in askNutritionCoach", e)
            "Sorry, there was an error connecting to NutriTrack AI Coach. Detail: ${e.localizedMessage}"
        }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize bitmap to avoid sending giant payloads to the API
        val scaled = if (bitmap.width > 800 || bitmap.height > 800) {
            val score = if (bitmap.width > bitmap.height) bitmap.width / 800f else bitmap.height / 800f
            Bitmap.createScaledBitmap(bitmap, (bitmap.width / score).toInt(), (bitmap.height / score).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}

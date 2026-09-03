package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class ProductApiResponse(
    val id: String? = null,
    val name: String,
    val category: String? = null,
    val carbon: String,
    val water: String,
    val ecoScore: Int = 50,
    val eco_score: Int? = null,
    val grade: String? = null,
    val decision: String? = null,
    val recommendation: String? = null,
    val recycling: String,
    val impact: String? = null,
    val alternative: String,
    val isEcoFriendly: Boolean? = null
)

data class SensorReadingPayload(
    val deviceName: String,
    val temperatureC: Float,
    val humidityPercent: Float,
    val co2Ppm: Float,
    val timestamp: Long
)

data class DatabaseSyncPayload(
    val products: List<ProductApiResponse>,
    val sensorReadings: List<SensorReadingPayload>,
    val timestamp: Long = System.currentTimeMillis()
)

// Gemini REST API Data Classes
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = "user"
)

data class GeminiPart(
    val text: String? = null
)

data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxOutputTokens: Int? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?,
    val modelVersion: String? = null
)

data class GeminiCandidate(
    val content: GeminiContent?,
    val finishReason: String? = null
)

interface EcoBackendApi {
    @GET("health")
    suspend fun checkHealth(): Map<String, Any>

    @GET("api/products/rfid/{uid}")
    suspend fun getProductByRfidUid(@Path("uid") uid: String): ProductApiResponse

    @GET("product/{id}")
    suspend fun getProductById(@Path("id") id: String): ProductApiResponse

    @GET("products")
    suspend fun getAllProducts(): List<ProductApiResponse>

    @POST("product")
    suspend fun createOrUpdateProduct(@Body product: ProductApiResponse): ProductApiResponse

    @PUT("product/{id}")
    suspend fun updateProductOnBackend(@Path("id") id: String, @Body product: ProductApiResponse): ProductApiResponse

    @POST("sensor-readings")
    suspend fun uploadSensorReadings(@Body readings: List<SensorReadingPayload>): Map<String, Any>

    @POST("sync-database")
    suspend fun syncDatabasePayload(@Body payload: DatabaseSyncPayload): Map<String, Any>
}

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentWithModel(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object NetworkClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 60-second timeouts recommended for Gemini AI API calls
    private val geminiOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Default REST Backend url (configurable in settings or ViewModel)
    fun createBackendApi(baseUrl: String = "http://10.0.2.2:3000/"): EcoBackendApi {
        val trimmed = baseUrl.trim()
        val formatted = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "http://$trimmed"
        }
        val safeUrl = if (formatted.endsWith("/")) formatted else "$formatted/"
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EcoBackendApi::class.java)
    }

    val geminiApi: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(geminiOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

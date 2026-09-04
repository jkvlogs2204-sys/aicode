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

// OpenAI ChatGPT REST API Data Classes
data class OpenAiMessage(
    val role: String,
    val content: String
)

data class OpenAiRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<OpenAiMessage>,
    val temperature: Float = 0.2f
)

data class OpenAiResponse(
    val choices: List<OpenAiChoice>?,
    val model: String? = null
)

data class OpenAiChoice(
    val message: OpenAiMessage?
)

interface OpenAiApiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse
}

object NetworkClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 60-second timeouts recommended for OpenAI API calls
    private val openAiOkHttpClient = OkHttpClient.Builder()
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

    val openAiApi: OpenAiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(openAiOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenAiApiService::class.java)
    }
}

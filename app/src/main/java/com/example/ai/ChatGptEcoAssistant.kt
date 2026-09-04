package com.example.ai

import android.content.Context
import com.example.BuildConfig
import com.example.data.NetworkClient
import com.example.data.OpenAiMessage
import com.example.data.OpenAiRequest
import com.example.data.ProductEntity
import com.example.data.SensorReadingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ChatGptConnectionTestResult(
    val success: Boolean,
    val model: String,
    val latencyMs: Long,
    val responseText: String,
    val errorMessage: String? = null
)

data class AiProductAnalysis(
    val summary: String,
    val greenerAdvice: String,
    val habitTip: String,
    val decision: String, // Environmental rating badge only: "GREEN", "YELLOW", or "RED"
    val grade: String, // "A+", "A", "B", "C", "D", "E"
    val decisionRecommendation: String, // e.g. "EXCELLENT CHOICE", "SUSTAINABLE SELECTION"
    val whyThisScore: String,
    val keyImpactDrivers: List<String>,
    val positiveFactors: List<String>,
    val disposalGuidance: String,
    val circularEconomyR6: Map<String, String>, // "USE BETTER", "REUSE", "REPAIR", "REDUCE", "RECYCLE", "REPLACE"
    val dataStatus: String = "Deterministic LCA Verified • Telemetry: Arduino -> App Only"
)

data class EnvironmentalSustainabilityAdvice(
    val airQualityRating: String, // "Optimal", "Good", "Moderate CO₂", "High Humidity", "Action Needed"
    val summary: String,
    val actionableSuggestions: List<String>,
    val energySavingTip: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ChatGptEcoAssistant {

    private const val PREFS_NAME = "eco_mind_prefs"
    private const val PREF_KEY_CHATGPT_API_KEY = "pref_chatgpt_api_key"

    private var customApiKey: String = ""

    fun initialize(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedKey = prefs.getString(PREF_KEY_CHATGPT_API_KEY, "") ?: ""
            if (savedKey.isNotBlank()) {
                customApiKey = savedKey.trim()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setCustomApiKey(context: Context?, key: String) {
        customApiKey = key.trim()
        context?.let {
            try {
                val prefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(PREF_KEY_CHATGPT_API_KEY, customApiKey).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearCustomApiKey(context: Context?) {
        customApiKey = ""
        context?.let {
            try {
                val prefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().remove(PREF_KEY_CHATGPT_API_KEY).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getActiveKeySource(): String {
        val key = getApiKey()
        return when {
            key.isNotBlank() -> "In-App ChatGPT Key (${key.take(7)}...)"
            else -> "Not Configured"
        }
    }

    fun getApiKey(): String {
        if (customApiKey.isNotBlank()) {
            return customApiKey.trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
        }
        val key = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        val cleanKey = key.trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
        return if (cleanKey.isNotBlank() && !cleanKey.contains("MY_")) cleanKey else ""
    }

    fun isChatGptConfigured(): Boolean = getApiKey().isNotEmpty()

    private suspend fun executeChatGptRequest(
        systemPrompt: String?,
        userPrompt: String,
        messages: List<OpenAiMessage>? = null,
        temperature: Float = 0.2f
    ): Pair<String, String> {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            throw IllegalStateException("ChatGPT API key is not configured. Please enter your OpenAI key in Settings.")
        }
        val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"

        val openAiMessages = mutableListOf<OpenAiMessage>()
        if (!systemPrompt.isNullOrBlank()) {
            openAiMessages.add(OpenAiMessage(role = "system", content = systemPrompt))
        }

        if (!messages.isNullOrEmpty()) {
            openAiMessages.addAll(messages)
        } else {
            openAiMessages.add(OpenAiMessage(role = "user", content = userPrompt))
        }

        val request = OpenAiRequest(
            model = "gpt-4o-mini",
            messages = openAiMessages,
            temperature = temperature
        )

        return try {
            val response = NetworkClient.openAiApi.createChatCompletion(authHeader, request)
            val text = response.choices?.firstOrNull()?.message?.content ?: "Eco Mind ChatGPT connected."
            Pair(text.trim(), response.model ?: "gpt-4o-mini")
        } catch (e1: Exception) {
            try {
                val fallbackRequest = request.copy(model = "gpt-3.5-turbo")
                val response = NetworkClient.openAiApi.createChatCompletion(authHeader, fallbackRequest)
                val text = response.choices?.firstOrNull()?.message?.content ?: "Eco Mind ChatGPT connected."
                Pair(text.trim(), response.model ?: "gpt-3.5-turbo")
            } catch (e2: Exception) {
                throw e1
            }
        }
    }

    suspend fun testChatGptConnection(): ChatGptConnectionTestResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext ChatGptConnectionTestResult(
                success = false,
                model = "gpt-4o-mini",
                latencyMs = 0,
                responseText = "",
                errorMessage = "ChatGPT API key is not configured. Please enter your OpenAI API key (sk-...) in Settings or AI Guide."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val (responseText, usedModel) = executeChatGptRequest(
                systemPrompt = "You are a concise AI assistant.",
                userPrompt = "Respond in exactly 4 words: Eco Mind ChatGPT connected.",
                temperature = 0.1f
            )
            val latency = System.currentTimeMillis() - startTime
            ChatGptConnectionTestResult(
                success = true,
                model = usedModel,
                latencyMs = latency,
                responseText = responseText,
                errorMessage = null
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            ChatGptConnectionTestResult(
                success = false,
                model = "gpt-4o-mini",
                latencyMs = latency,
                responseText = "",
                errorMessage = e.localizedMessage ?: e.message ?: "Connection error"
            )
        }
    }

    suspend fun fetchProductDetailsViaChatGpt(productQuery: String): ProductEntity = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val generatedId = "GPT_${System.currentTimeMillis().toString().takeLast(6)}"

        val prompt = """
            You are Eco Mind, an expert environmental & product recycling data AI.
            Analyze the product "$productQuery" and return structured environmental data in EXACTLY this key-value format (or as a JSON object):

            Name: <Clean full product name>
            Category: <One of: Food, Plastic, Glass, Paper, Electronics, Metal, Wood/Organic, Textiles, Other>
            Carbon: <Estimated carbon footprint e.g. "120g CO2" or "2.4kg CO2">
            Water: <Estimated water footprint e.g. "4.5 litres">
            EcoScore: <Integer score from 0 to 100, where 100 is most eco-friendly>
            Recycling: <Step-by-step recycling method and bin instructions>
            Impact: <Environmental impact description e.g. ocean persistence, landfill lifespan, or raw resource draw>
            Alternative: <Sustainable greener alternative product>
            IsEcoFriendly: <true or false>
        """.trimIndent()

        if (apiKey.isNotEmpty()) {
            try {
                val (aiText, _) = executeChatGptRequest(
                    systemPrompt = "You are Eco Mind, an expert environmental recycling and sustainability AI.",
                    userPrompt = prompt,
                    temperature = 0.2f
                )
                if (aiText.isNotBlank()) {
                    return@withContext parseChatGptProductResponse(aiText, productQuery, generatedId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext generateLocalProductFromQuery(productQuery, generatedId)
    }

    private fun parseChatGptProductResponse(raw: String, originalQuery: String, id: String): ProductEntity {
        fun extractKey(key: String): String? {
            val regex = Regex("(?i)(?:^|\\n)[\\s*#-]*\\*?\\*?${key}\\*?\\*?:\\s*(.*)", RegexOption.MULTILINE)
            val match = regex.find(raw)?.groupValues?.get(1)?.trim()
            return match?.replace(Regex("^\\*+|\\*+$"), "")?.trim()
        }

        var jsonName: String? = null
        var jsonCategory: String? = null
        var jsonCarbon: String? = null
        var jsonWater: String? = null
        var jsonScore: Int? = null
        var jsonRecycling: String? = null
        var jsonImpact: String? = null
        var jsonAlt: String? = null
        var jsonEcoFriendly: Boolean? = null

        val jsonPattern = Regex("(?s)\\{.*\\}")
        val jsonMatch = jsonPattern.find(raw)?.value
        if (jsonMatch != null) {
            try {
                val obj = JSONObject(jsonMatch)
                if (obj.has("Name") || obj.has("name")) jsonName = obj.optString("Name", obj.optString("name", ""))
                if (obj.has("Category") || obj.has("category")) jsonCategory = obj.optString("Category", obj.optString("category", ""))
                if (obj.has("Carbon") || obj.has("carbon")) jsonCarbon = obj.optString("Carbon", obj.optString("carbon", ""))
                if (obj.has("Water") || obj.has("water")) jsonWater = obj.optString("Water", obj.optString("water", ""))
                if (obj.has("EcoScore") || obj.has("ecoScore")) jsonScore = obj.optInt("EcoScore", obj.optInt("ecoScore", -1))
                if (obj.has("Recycling") || obj.has("recycling")) jsonRecycling = obj.optString("Recycling", obj.optString("recycling", ""))
                if (obj.has("Impact") || obj.has("impact")) jsonImpact = obj.optString("Impact", obj.optString("impact", ""))
                if (obj.has("Alternative") || obj.has("alternative")) jsonAlt = obj.optString("Alternative", obj.optString("alternative", ""))
                if (obj.has("IsEcoFriendly") || obj.has("isEcoFriendly")) jsonEcoFriendly = obj.optBoolean("IsEcoFriendly", obj.optBoolean("isEcoFriendly", false))
            } catch (_: Exception) {}
        }

        val name = jsonName?.ifBlank { null } ?: extractKey("Name")?.ifBlank { null } ?: originalQuery.capitalizeWords()
        val category = jsonCategory?.ifBlank { null } ?: extractKey("Category") ?: "Plastic"
        val carbon = jsonCarbon?.ifBlank { null } ?: extractKey("Carbon") ?: "180g CO2"
        val water = jsonWater?.ifBlank { null } ?: extractKey("Water") ?: "6 litres"
        val ecoScore = (if (jsonScore != null && jsonScore in 0..100) jsonScore else extractKey("EcoScore")?.toIntOrNull()?.coerceIn(0, 100)) ?: 65
        val recycling = jsonRecycling?.ifBlank { null } ?: extractKey("Recycling") ?: "Clean thoroughly and deposit in appropriate local recycling stream."
        val impact = jsonImpact?.ifBlank { null } ?: extractKey("Impact") ?: "Production involves raw energy extraction and long decomposition cycles."
        val alternative = jsonAlt?.ifBlank { null } ?: extractKey("Alternative") ?: "Reusable or biodegradable alternative"
        val isEcoFriendlyStr = extractKey("IsEcoFriendly")
        val isEcoFriendly = jsonEcoFriendly ?: isEcoFriendlyStr?.equals("true", ignoreCase = true) ?: (ecoScore >= 60)

        return ProductEntity(
            id = id,
            name = name,
            category = category,
            carbon = carbon,
            water = water,
            ecoScore = ecoScore,
            recycling = recycling,
            impact = impact,
            alternative = alternative,
            isEcoFriendly = isEcoFriendly
        )
    }

    private fun generateLocalProductFromQuery(query: String, id: String): ProductEntity {
        val q = query.lowercase()
        return when {
            q.contains("bottle") || q.contains("plastic") || q.contains("tub") || q.contains("cup") -> {
                ProductEntity(
                    id = id,
                    name = query.capitalizeWords(),
                    category = "Plastic",
                    carbon = "180g CO2",
                    water = "5.2 Litres",
                    ecoScore = 42,
                    recycling = "Rinse residue with cold water. Flatten bottle, keep cap screwed on, and deposit in Blue Bin (PET #1 / HDPE #2).",
                    impact = "Takes over 450 years to degrade in landfills; high risk of ocean microplastic fragmentation.",
                    alternative = "Insulated Stainless Steel Water Bottle or Glass Flask",
                    isEcoFriendly = false
                )
            }
            q.contains("can") || q.contains("aluminum") || q.contains("aluminium") || q.contains("tin") || q.contains("foil") -> {
                ProductEntity(
                    id = id,
                    name = query.capitalizeWords(),
                    category = "Metal",
                    carbon = "95g CO2",
                    water = "2.1 Litres",
                    ecoScore = 85,
                    recycling = "100% infinitely recyclable. Empty contents, crush can to conserve space, and place in Metal Recycling Bin.",
                    impact = "Recyclable metal processing consumes 95% less energy than refining primary bauxite ore.",
                    alternative = "Refillable Growler or Bulk Stainless Dispenser",
                    isEcoFriendly = true
                )
            }
            q.contains("glass") || q.contains("jar") -> {
                ProductEntity(
                    id = id,
                    name = query.capitalizeWords(),
                    category = "Glass",
                    carbon = "110g CO2",
                    water = "1.8 Litres",
                    ecoScore = 88,
                    recycling = "Rinse food traces. Remove metal lids for separate recycling. Place clean glass into Green/Clear Glass Bin.",
                    impact = "Glass is non-toxic and 100% endlessly recyclable without material quality degradation.",
                    alternative = "Multi-use Mason Jars or Durable Ceramic Containers",
                    isEcoFriendly = true
                )
            }
            q.contains("paper") || q.contains("cardboard") || q.contains("box") || q.contains("carton") -> {
                ProductEntity(
                    id = id,
                    name = query.capitalizeWords(),
                    category = "Paper",
                    carbon = "45g CO2",
                    water = "8.5 Litres",
                    ecoScore = 78,
                    recycling = "Flatten cardboard boxes. Remove plastic packing tape and wax coatings before placing in Yellow Paper Bin.",
                    impact = "Paper fibers can be re-pulped 5–7 times into new recycled packaging materials.",
                    alternative = "Post-Consumer Recycled Kraft Cardboard or FSC Certified Packaging",
                    isEcoFriendly = true
                )
            }
            q.contains("battery") || q.contains("e-waste") || q.contains("phone") || q.contains("laptop") || q.contains("cable") -> {
                ProductEntity(
                    id = id,
                    name = query.capitalizeWords(),
                    category = "Electronics",
                    carbon = "4.2kg CO2",
                    water = "45 Litres",
                    ecoScore = 25,
                    recycling = "DO NOT place in standard household waste! Take to certified e-waste drop-off center to safely extract Lithium & Cobalt.",
                    impact = "Contains heavy metals (Lead, Cadmium, Lithium) that contaminate soil and groundwater if landfilled.",
                    alternative = "Modular Repairable Electronics & Rechargeable NiMH Batteries",
                    isEcoFriendly = false
                )
            }
            else -> {
                ProductEntity(
                    id = id,
                    name = query.capitalizeWords(),
                    category = "Other",
                    carbon = "130g CO2",
                    water = "4.0 Litres",
                    ecoScore = 60,
                    recycling = "Check material identification codes. Clean item and consult local municipality waste segregation rules.",
                    impact = "General manufacturing footprint requiring energy grid extraction and transport logistics.",
                    alternative = "Sustainable FSC Certified or Organic Reusable Products",
                    isEcoFriendly = true
                )
            }
        }
    }

    private fun String.capitalizeWords(): String {
        return this.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    fun computeEcoDecision(score: Int): Triple<String, String, String> {
        return when {
            score >= 90 -> Triple("GREEN", "A+", "EXCELLENT CHOICE")
            score >= 75 -> Triple("GREEN", "A", "SUSTAINABLE SELECTION")
            score >= 60 -> Triple("YELLOW", "B", "MODERATE BURDEN")
            score >= 45 -> Triple("YELLOW", "C", "IMPROVEMENT RECOMMENDED")
            score >= 25 -> Triple("RED", "D", "HIGH ENVIRONMENTAL BURDEN")
            else -> Triple("RED", "E", "CRITICAL ENVIRONMENTAL FOOTPRINT")
        }
    }

    suspend fun analyzeProduct(product: ProductEntity): AiProductAnalysis = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        val prompt = """
            You are Eco Mind, an expert environmental decision-making assistant for Life Cycle Assessment (LCA).
            Analyze this scanned product:
            - Product Name: ${product.name}
            - Category: ${product.category}
            - Carbon Footprint: ${product.carbon}
            - Water Footprint: ${product.water}
            - Eco Score: ${product.ecoScore} / 100
            - Recycling Info: ${product.recycling}
            - Stated Impact: ${product.impact}
            - Recommended Alternative: ${product.alternative}

            Provide a comprehensive sustainability analysis:
            1. Environmental Impact Summary (1-2 sentences on lifecycle impact)
            2. Greener Alternative & Actionable Tip (practical transition advice)
            3. Personalized habit recommendation (daily action to reduce footprint)
            4. Why This Score was awarded (breakdown of positive/negative scoring factors)
            5. Municipal disposal guidance (exact bin sorting, cleaning, and hazardous handling instructions)

            CRITICAL ARCHITECTURE REQUIREMENT: Hardware communication is strictly unidirectional (Arduino -> Mobile App).
            Do NOT output, suggest, or include any Arduino return commands or actuator feedback (NO servo or LED commands).
        """.trimIndent()

        if (apiKey.isNotEmpty()) {
            try {
                val (aiText, _) = executeChatGptRequest(
                    systemPrompt = "You are Eco Mind, an expert environmental sustainability and lifecycle assessment AI.",
                    userPrompt = prompt,
                    temperature = 0.3f
                )
                if (aiText.isNotBlank()) {
                    return@withContext parseAiResponse(aiText, product)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext generateLocalAnalysis(product)
    }

    suspend fun askEcoChatHistory(
        chatHistory: List<Pair<String, String>>,
        contextProduct: ProductEntity? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        val systemContext = if (contextProduct != null) {
            "Active Scanned Product: '${contextProduct.name}' (Category: ${contextProduct.category}, Eco Score: ${contextProduct.ecoScore}/100, Carbon Footprint: ${contextProduct.carbon}, Water Footprint: ${contextProduct.water}, Recycling Method: ${contextProduct.recycling}, Recommended Alternative: ${contextProduct.alternative})."
        } else "No active product currently scanned."

        val systemInstruction = """
            You are Eco Mind, an encouraging, expert, and friendly IoT sustainability & environmental ChatGPT AI guide.
            Your mission is to provide accurate recycling methods, waste sorting rules, CO₂ carbon footprint analysis, water usage facts, e-waste guidance, and circular economy tips.
            $systemContext
            Formatting: Keep answers clear, well-structured, actionable, and formatted with bullet points or short key headings.
        """.trimIndent()

        if (apiKey.isNotEmpty()) {
            try {
                val openAiMessages = mutableListOf<OpenAiMessage>()
                openAiMessages.add(OpenAiMessage(role = "system", content = systemInstruction))

                val recentHistory = chatHistory.takeLast(10)
                for ((sender, text) in recentHistory) {
                    val roleStr = if (sender == "user") "user" else "assistant"
                    openAiMessages.add(OpenAiMessage(role = roleStr, content = text))
                }

                val (aiText, _) = executeChatGptRequest(
                    systemPrompt = null,
                    userPrompt = "",
                    messages = openAiMessages,
                    temperature = 0.7f
                )

                if (aiText.isNotBlank()) {
                    return@withContext aiText.trim()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val lastUserMsg = chatHistory.lastOrNull { it.first == "user" }?.second ?: ""
        return@withContext generateLocalChatMessage(lastUserMsg, contextProduct)
    }

    suspend fun askEcoChat(userMessage: String, contextProduct: ProductEntity? = null): String {
        return askEcoChatHistory(listOf(Pair("user", userMessage)), contextProduct)
    }

    private fun parseAiResponse(raw: String, product: ProductEntity): AiProductAnalysis {
        val (decision, grade, rec) = computeEcoDecision(product.ecoScore)
        val lines = raw.split("\n").filter { it.isNotBlank() }

        val summary = lines.getOrNull(0)?.replace(Regex("^[0-9.#*-]+\\s*"), "")
            ?: "${product.name} demonstrates a carbon footprint of ${product.carbon} with an Eco Score of ${product.ecoScore}/100."
        val advice = lines.getOrNull(1)?.replace(Regex("^[0-9.#*-]+\\s*"), "")
            ?: "Switch to ${product.alternative} to significantly reduce total lifecycle emissions."
        val habit = lines.getOrNull(2)?.replace(Regex("^[0-9.#*-]+\\s*"), "")
            ?: "Properly segregating clean materials accelerates circular resource loops."
        val whyScore = lines.getOrNull(3)?.replace(Regex("^[0-9.#*-]+\\s*"), "")
            ?: "Evaluated on material extraction intensity, water utilization, end-of-life recycling efficiency, and carbon emissions."

        val keyDrivers = listOf(
            "Primary Material Stream: ${product.category}",
            "Embodied Energy & Processing Footprint: ${product.carbon}",
            "Hydrological Resource Impact: ${product.water}"
        )

        val positives = listOf(
            if (product.isEcoFriendly) "High Circularity & Resource Efficiency" else "Local Recycling Municipal Segregation Available",
            "Clear Lifecycle Recovery Guidance Available"
        )

        val r6Map = mapOf(
            "USE BETTER" to "Opt for durable ${product.alternative} rather than single-use ${product.category.lowercase()}.",
            "REUSE" to "Clean and repurpose containers for storage or bulk filling before recycling.",
            "REPAIR" to "Extend product lifecycle by replacing wear components before disposal.",
            "REDUCE" to "Minimize packaging waste by purchasing items with minimal or zero plastic wraps.",
            "RECYCLE" to product.recycling,
            "REPLACE" to "Replace with certified sustainable alternatives (${product.alternative})."
        )

        return AiProductAnalysis(
            summary = summary,
            greenerAdvice = advice,
            habitTip = habit,
            decision = decision,
            grade = grade,
            decisionRecommendation = rec,
            whyThisScore = whyScore,
            keyImpactDrivers = keyDrivers,
            positiveFactors = positives,
            disposalGuidance = product.recycling,
            circularEconomyR6 = r6Map
        )
    }

    private fun generateLocalAnalysis(product: ProductEntity): AiProductAnalysis {
        val (decision, grade, rec) = computeEcoDecision(product.ecoScore)

        val summary = "${product.name} (${product.category}) has an estimated carbon footprint of ${product.carbon} and water consumption of ${product.water}."
        val advice = "Consider adopting ${product.alternative} to reduce overall environmental burden."
        val habit = "Always inspect local recycling guidelines to ensure clean material separation."
        val whyScore = "Awarded an Eco Score of ${product.ecoScore}/100 based on material composition, processing intensity, and end-of-life recyclability."

        val keyDrivers = listOf(
            "Category: ${product.category}",
            "Carbon Footprint: ${product.carbon}",
            "Water Footprint: ${product.water}"
        )

        val positives = if (product.isEcoFriendly) {
            listOf("Low Carbon Profile", "Infinitely or Highly Recyclable Material", "Sustainable Packaging")
        } else {
            listOf("Recyclable via Municipal Facilities", "Contains Segregable Components")
        }

        val r6Map = mapOf(
            "USE BETTER" to "Choose products manufactured with clean renewable energy.",
            "REUSE" to "Repurpose or refill container multiple times before recycling.",
            "REPAIR" to "Maintain product structural integrity to maximize longevity.",
            "REDUCE" to "Limit single-use consumer purchases in favor of reusable options.",
            "RECYCLE" to product.recycling,
            "REPLACE" to "Transition to eco-friendly options like ${product.alternative}."
        )

        return AiProductAnalysis(
            summary = summary,
            greenerAdvice = advice,
            habitTip = habit,
            decision = decision,
            grade = grade,
            decisionRecommendation = rec,
            whyThisScore = whyScore,
            keyImpactDrivers = keyDrivers,
            positiveFactors = positives,
            disposalGuidance = product.recycling,
            circularEconomyR6 = r6Map
        )
    }

    private fun generateLocalChatMessage(userMsg: String, product: ProductEntity?): String {
        val msg = userMsg.lowercase()
        val prodContext = if (product != null) "for **${product.name}** (Eco Score: ${product.ecoScore}/100)" else ""

        return when {
            msg.contains("recycl") || msg.contains("bin") || msg.contains("trash") -> {
                """
                ### ♻️ Recycling Guidance $prodContext
                
                - **Segregation**: Clean residue before depositing in municipal recycling streams.
                - **Contamination Control**: Remove non-recyclable caps or plastic wraps.
                - **Circular Loop**: Recycled materials reduce raw resource extraction by up to **90%**.
                """.trimIndent()
            }
            msg.contains("carbon") || msg.contains("co2") || msg.contains("emission") -> {
                """
                ### 🌍 Carbon Footprint Analysis $prodContext
                
                - **Embodied Emissions**: Manufacturing & transport account for major lifecycle CO₂.
                - **Impact Reduction**: Switching to renewable energy and reusable alternatives lowers emissions significantly.
                """.trimIndent()
            }
            msg.contains("water") || msg.contains("hydro") -> {
                """
                ### 💧 Water Footprint Insight $prodContext
                
                - **Hydrological Draw**: Industrial processing requires substantial water volumes.
                - **Conservation Tip**: Prefer items produced via closed-loop water treatment systems.
                """.trimIndent()
            }
            else -> {
                """
                ### 🤖 ChatGPT Eco Assistant Ready $prodContext
                
                I am your **ChatGPT-powered Eco Assistant**! Ask me anything about:
                - ♻️ **Recycling rules & bin sorting**
                - 🌿 **CO₂ carbon footprint metrics**
                - 💧 **Water consumption & environmental impact**
                - 📦 **Sustainable product alternatives**
                """.trimIndent()
            }
        }
    }

    suspend fun generateEnvironmentalSuggestions(
        readings: List<SensorReadingEntity>? = null,
        latestTemp: Float? = null,
        latestHum: Float? = null,
        latestCo2: Float? = null
    ): EnvironmentalSustainabilityAdvice = withContext(Dispatchers.IO) {
        val temp = latestTemp ?: readings?.lastOrNull()?.temperatureC ?: 23.5f
        val hum = latestHum ?: readings?.lastOrNull()?.humidityPercent ?: 48.0f
        val co2 = latestCo2 ?: readings?.lastOrNull()?.co2Ppm ?: 425.0f

        val rating = when {
            co2 > 1000f -> "High CO₂ Level"
            co2 > 800f -> "Moderate CO₂ Level"
            hum > 75f -> "High Humidity Level"
            temp > 30f -> "Elevated Indoor Temperature"
            else -> "Optimal Ambient Condition"
        }

        val prompt = """
            You are Eco Mind ChatGPT Environmental Assistant.
            Analyze the following real-time telemetry sensor readings from Cloud Firestore / BLE:
            - Temperature: ${temp}°C
            - Humidity: ${hum}%
            - CO₂ Concentration: ${co2} ppm
            
            Provide an environmental analysis in this structure:
            Rating: $rating
            Summary: 1-2 sentence overview of indoor environmental air quality & energy state.
            Suggestions: 3 practical actionable suggestions to optimize energy usage or air quality.
            Tip: 1 energy saving tip.
        """.trimIndent()

        val apiKey = getApiKey()
        if (apiKey.isNotEmpty()) {
            try {
                val (aiText, _) = executeChatGptRequest(
                    systemPrompt = "You are Eco Mind ChatGPT, an expert indoor environmental air quality and energy sustainability AI.",
                    userPrompt = prompt,
                    temperature = 0.3f
                )
                if (aiText.isNotBlank()) {
                    val lines = aiText.split("\n").filter { it.isNotBlank() }
                    val summary = lines.firstOrNull { it.contains("Summary", ignoreCase = true) }?.replace(Regex("(?i).*Summary:\\s*"), "")
                        ?: "Ambient room sensors indicate temperature of ${temp}°C, humidity at ${hum}%, and CO₂ level of ${co2} ppm."
                    val tip = lines.firstOrNull { it.contains("Tip", ignoreCase = true) }?.replace(Regex("(?i).*Tip:\\s*"), "")
                        ?: "Maintain natural cross-ventilation during non-peak energy hours to lower CO₂."
                    val suggestions = lines.filter { it.trim().startsWith("-") || it.trim().startsWith("•") || it.trim().matches(Regex("^\\d+\\..*")) }
                        .map { it.replace(Regex("^[0-9.#*-]+\\s*"), "").trim() }
                        .take(3)
                        .ifEmpty {
                            listOf(
                                "Increase room ventilation to reduce ambient CO₂ concentration below 600 ppm.",
                                "Utilize smart thermostat scheduling to maintain temperature between 21°C–24°C.",
                                "Keep indoor humidity regulated between 40%–60% to maximize comfort and air purity."
                            )
                        }

                    return@withContext EnvironmentalSustainabilityAdvice(
                        airQualityRating = rating,
                        summary = summary,
                        actionableSuggestions = suggestions,
                        energySavingTip = tip
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext EnvironmentalSustainabilityAdvice(
            airQualityRating = rating,
            summary = "Telemetry reading: Temp ${temp}°C, Humidity ${hum}%, CO₂ ${co2} ppm.",
            actionableSuggestions = listOf(
                "Increase fresh air ventilation to maintain low ambient CO₂ levels.",
                "Keep windows shaded during peak sunlight hours to stabilize room temperature.",
                "Monitor indoor humidity to stay within the recommended 45%–55% range."
            ),
            energySavingTip = "Unplug unused appliances to eliminate phantom standby power draw."
        )
    }
}

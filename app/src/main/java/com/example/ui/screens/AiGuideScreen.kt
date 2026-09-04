package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatMessage
import com.example.ui.EcoMindViewModel
import com.example.ui.components.FootprintComparisonChart
import com.example.ui.components.ChatGptApiKeyDialog
import com.example.ui.components.RealtimeLineChartCanvas
import com.example.ui.theme.EcoBadgeBad
import com.example.ui.theme.EcoBadgeGood
import com.example.ui.theme.EcoBadgeWarning

@Composable
fun AiGuideScreen(
    viewModel: EcoMindViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val bleSensorData by viewModel.bleLatestSensorData.collectAsState()
    val roomSensorHistory by viewModel.roomSensorHistory.collectAsState()
    val chatGptConnectionStatus by viewModel.chatGptConnectionStatus.collectAsState()
    val isTestingChatGpt by viewModel.isTestingChatGpt.collectAsState()
    val isChatGptConfigured by viewModel.isChatGptConfigured.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showChartsSection by remember { mutableStateOf(false) }
    var showChatGptKeyDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isChatLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val samplePrompts = listOf(
        "Which plastic bottles can be recycled locally?",
        "How to safely dispose of e-waste batteries?",
        "What is carbon footprint in product manufacturing?",
        "Tips for reducing single-use packaging waste",
        "How does RFID recycling classification work?"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("ai_header_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF16261F),
                                    Color(0xFF1E3A2F),
                                    Color(0xFF0D1C15)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EcoBadgeGood.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EcoBadgeGood.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EcoBadgeGood, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CHATGPT GPT-4O-MINI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EcoBadgeGood)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { showChatGptKeyDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("btn_configure_chatgpt_key")
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isChatGptConfigured) EcoBadgeGood else EcoBadgeWarning)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isChatGptConfigured) "Key Active" else "Set Key",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChatGptConfigured) EcoBadgeGood else EcoBadgeWarning
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                OutlinedButton(
                                    onClick = { viewModel.testChatGptApiLive() },
                                    enabled = !isTestingChatGpt,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("btn_test_chatgpt_api")
                                ) {
                                    if (isTestingChatGpt) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = EcoBadgeGood)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = EcoBadgeGood)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isTestingChatGpt) "Pinging..." else "Ping API",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoBadgeGood
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Eco Mind ChatGPT Guide",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )

                            Row {
                                OutlinedButton(
                                    onClick = { showChartsSection = !showChartsSection },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("btn_toggle_eco_charts")
                                ) {
                                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(12.dp), tint = EcoBadgeGood)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showChartsSection) "Hide Charts" else "Eco Charts",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoBadgeGood
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { viewModel.clearChatHistory() },
                                    modifier = Modifier.size(28.dp).testTag("btn_clear_chat")
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Text(
                            text = "Ask anything on recycling methods, carbon footprints, e-waste, or circular economy.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        // ChatGPT Live API Connectivity Info Banner
                        val currentStatus = chatGptConnectionStatus
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showChatGptKeyDialog = true }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (currentStatus?.success == true) EcoBadgeGood else EcoBadgeWarning)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentStatus?.success == true) {
                                    "OpenAI ChatGPT: ${currentStatus.model} • Latency: ${currentStatus.latencyMs}ms"
                                } else if (isChatGptConfigured) {
                                    "ChatGPT API: ${currentStatus?.errorMessage ?: "Testing connection..."} (Tap to manage key)"
                                } else {
                                    "ChatGPT API: Key not configured • Tap to set key"
                                },
                                fontSize = 10.sp,
                                color = if (currentStatus?.success == true) EcoBadgeGood.copy(alpha = 0.9f) else EcoBadgeWarning,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Active Product Scanned Context Banner
            AnimatedVisibility(visible = scannedProduct != null) {
                scannedProduct?.let { product ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().testTag("active_product_context_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("ACTIVE SCANNED PRODUCT:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.sendChatMessage("Explain the recycling method and carbon footprint for my scanned product '${product.name}'.")
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("btn_ask_scanned_product")
                            ) {
                                Text("Ask AI Guide", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Expandable Eco Analytics & Charts Panel (Sent directly to AI API)
            AnimatedVisibility(visible = showChartsSection) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("eco_charts_analysis_panel")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BarChart, contentDescription = null, tint = EcoBadgeGood, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Interactive Chart Analysis", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Auto-sends chart to AI API", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Chart 1: Product Carbon & Resource Intensity Comparison
                            val currentProd = scannedProduct ?: allProducts.firstOrNull()
                            val prodName = currentProd?.name ?: "Plastic Water Bottle"
                            val prodCarbon = currentProd?.carbon ?: "120g CO2e"
                            val prodWater = currentProd?.water ?: "3.5 Liters"
                            val prodScore = currentProd?.ecoScore ?: 45
                            val prodAlt = currentProd?.alternative ?: "Stainless Steel Flask"

                            FootprintComparisonChart(
                                productName = prodName,
                                carbonText = prodCarbon,
                                waterText = prodWater,
                                ecoScore = prodScore,
                                alternativeName = prodAlt
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    val chartPrompt = "Analyze this Carbon Footprint Chart: Product '$prodName' has Carbon: $prodCarbon, Water: $prodWater, Eco Score: $prodScore/100 vs Alternative '$prodAlt'. What are the key environmental takeaways and recycling steps?"
                                    viewModel.sendChatMessage(chartPrompt)
                                    showChartsSection = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EcoBadgeGood),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_send_footprint_chart_to_ai")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("💡 Send Footprint Chart to AI Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Chart 2: IoT Environmental Sensor Telemetry Chart
                            val tempVal = bleSensorData?.temperatureC ?: roomSensorHistory.firstOrNull()?.temperatureC ?: 24.5f
                            val humVal = bleSensorData?.humidityPercent ?: roomSensorHistory.firstOrNull()?.humidityPercent ?: 55.0f
                            val co2Val = bleSensorData?.co2Ppm ?: roomSensorHistory.firstOrNull()?.co2Ppm ?: 480.0f

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth().padding(4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("IoT Sensor Chart Metrics", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("${tempVal}°C | ${humVal}% | ${co2Val.toInt()} PPM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            val chartPrompt = "Analyze our IoT Environmental Sensor Telemetry Chart: Current Temperature is ${tempVal}°C, Relative Humidity is ${humVal}%, and Indoor CO2 Concentration is ${co2Val.toInt()} PPM. Are these readings optimal for sustainability and health? Provide actionable recommendations."
                                            viewModel.sendChatMessage(chartPrompt)
                                            showChartsSection = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("btn_send_sensor_chart_to_ai")
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🌡️ Send Telemetry Sensor Chart to AI Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Preset Prompt Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(samplePrompts) { prompt ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable {
                                viewModel.sendChatMessage(prompt)
                            }
                            .testTag("prompt_chip")
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chat History List
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(messages) { msg ->
                    ChatBubble(msg = msg)
                }

                if (isChatLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Eco Mind ChatGPT AI thinking...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chat Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Eco Assistant...", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendChatMessage(inputText.trim())
                                inputText = ""
                            }
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendChatMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isChatLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("ai_chat_send_button")
                ) {
                    if (isChatLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = msg.text,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "ChatGPT gpt-4o-mini",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

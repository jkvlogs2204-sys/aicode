package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
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
import com.example.bluetooth.BluetoothState
import com.example.data.ProductEntity
import com.example.ui.EcoMindViewModel
import com.example.ui.components.EcoGaugeCanvas
import com.example.ui.components.FootprintComparisonChart
import com.example.ui.theme.EcoBadgeBad
import com.example.ui.theme.EcoBadgeGood
import com.example.ui.theme.EcoBadgeWarning

import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import com.example.ui.components.ProductEditDialog
import com.example.ui.components.RealtimeSensorStreamCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: EcoMindViewModel,
    onNavigateToHardware: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val bluetoothState by viewModel.bluetoothState.collectAsState()
    val lastRawData by viewModel.lastRawData.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val lastFeedback by viewModel.lastArduinoFeedback.collectAsState()
    val bleSensorData by viewModel.bleLatestSensorData.collectAsState()
    val telemetryHistory by viewModel.telemetryHistory.collectAsState()
    val roomSensorHistory by viewModel.roomSensorHistory.collectAsState()
    val isBackendSyncing by viewModel.isBackendSyncing.collectAsState()
    val backendSyncStatus by viewModel.backendSyncStatus.collectAsState()
    val activeTagMapping by viewModel.activeTagMapping.collectAsState()
    val sustainabilityAdvice by viewModel.sustainabilityAdvice.collectAsState()
    val isGeneratingSustainabilityAdvice by viewModel.isGeneratingSustainabilityAdvice.collectAsState()

    var customTagInput by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showHc05Dialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showHc05Dialog) {
        BluetoothConnectionScreen(
            viewModel = viewModel,
            onNavigateToDashboard = { showHc05Dialog = false },
            onBack = { showHc05Dialog = false }
        )
    }

    if (showEditDialog && scannedProduct != null) {
        ProductEditDialog(
            product = scannedProduct!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProduct, syncToBackend ->
                viewModel.updateProduct(updatedProduct, syncToBackend)
                showEditDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 0. Hero Aesthetic Banner
        HeroAestheticBanner(
            roomRecordCount = roomSensorHistory.size
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Connection Header & Status Banner
        ConnectionStatusHeader(
            state = bluetoothState,
            lastRawData = lastRawData,
            onConnectClick = { showHc05Dialog = true },
            onManageClick = onNavigateToHardware
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Real-Time Arduino Sensor Stream Card (Gauges & Real-time Canvas Line Chart with Room DB Persistence)
        RealtimeSensorStreamCard(
            sensorData = bleSensorData,
            telemetryHistory = telemetryHistory,
            roomSensorHistory = roomSensorHistory,
            rawSerialData = lastRawData,
            onSaveSnapshotToRoom = {
                val temp = bleSensorData?.temperatureC ?: telemetryHistory.lastOrNull()?.tempC ?: 24.2f
                val hum = bleSensorData?.humidityPercent ?: telemetryHistory.lastOrNull()?.humidityPercent ?: 50.0f
                val co2 = telemetryHistory.lastOrNull()?.co2Ppm ?: 412f
                viewModel.logSensorReadingToRoom(temp, hum, co2)
            },
            onClearRoomHistory = {
                viewModel.clearRoomSensorHistory()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2a. Gemini AI Environmental Sustainability Suggestions Card (Firestore Data Source)
        GeminiSustainabilityAdviceCard(
            advice = sustainabilityAdvice,
            isLoading = isGeneratingSustainabilityAdvice,
            onRefreshAdvice = { viewModel.generateSustainabilityAdviceFromFirestore() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2b. Remote Backend Database Sync Control Card
        BackendDatabaseSyncCard(
            isSyncing = isBackendSyncing,
            syncStatus = backendSyncStatus,
            onSyncFullDatabase = { viewModel.syncFullDatabaseToBackend() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Manual RFID Tag Input Panel
        RfidTagManualEntryBar(
            onScanTag = { tag -> viewModel.manualRfidScan(tag) },
            customInput = customTagInput,
            onInputChange = { customTagInput = it },
            onSubmitCustom = {
                if (customTagInput.isNotBlank()) {
                    viewModel.manualRfidScan(customTagInput.trim())
                    customTagInput = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Scanned Product Eco Information Dashboard
        if (scannedProduct != null) {
            ProductDashboardCard(
                product = scannedProduct!!,
                activeTagMapping = activeTagMapping,
                onRefreshAi = { viewModel.generateAiAnalysis(scannedProduct!!) },
                onUpdateProductClick = { showEditDialog = true },
                isAiLoading = isAiLoading,
                aiAnalysis = aiAnalysis
            )
        } else {
            EmptyScanPlaceholder()
        }
    }
}

@Composable
private fun ConnectionStatusHeader(
    state: BluetoothState,
    lastRawData: String,
    onConnectClick: () -> Unit,
    onManageClick: () -> Unit
) {
    val isConnected = state is BluetoothState.Connected
    val isConnecting = state is BluetoothState.Connecting || state is BluetoothState.Reconnecting
    val isError = state is BluetoothState.Error

    val (statusColor, statusTitle, statusSubtitle) = when (state) {
        is BluetoothState.Connected -> Triple(
            EcoBadgeGood,
            "● HC-05 CONNECTED",
            "RFID Scanner Active • ${state.deviceName}"
        )
        is BluetoothState.Connecting -> Triple(
            EcoBadgeWarning,
            "● CONNECTING TO HC-05...",
            "Connecting to ${state.deviceName}..."
        )
        is BluetoothState.Reconnecting -> Triple(
            EcoBadgeWarning,
            "● RECONNECTING TO HC-05...",
            "Attempting auto-reconnect to ${state.deviceName}..."
        )
        is BluetoothState.Error -> Triple(
            EcoBadgeBad,
            "● HC-05 DISCONNECTED",
            state.message
        )
        else -> Triple(
            Color.Gray,
            "● HC-05 DISCONNECTED",
            "RFID Scanner is not connected. Tap button to setup connection."
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnectClick() }
            .testTag("bluetooth_status_header")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = statusTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (lastRawData.isNotBlank() && isConnected) "Serial Stream: $lastRawData" else statusSubtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isConnected) {
                OutlinedButton(
                    onClick = onConnectClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("hardware_setup_button")
                ) {
                    Text("MANAGE SCANNER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onConnectClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("hardware_setup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CONNECT RFID SCANNER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RfidTagManualEntryBar(
    onScanTag: (String) -> Unit,
    customInput: String,
    onInputChange: (String) -> Unit,
    onSubmitCustom: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RFID Tag Entry & Hardware Quick Scan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val presetTags = listOf(
                    "1001" to "Plastic Bottle",
                    "1002" to "Steel Bottle",
                    "1003" to "Organic Shirt",
                    "1006" to "Bamboo Toothbrush",
                    "1007" to "Battery",
                    "1014" to "Fast Food",
                    "1019" to "Smartphone"
                )

                presetTags.forEach { (tagId, name) ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable { onScanTag(tagId) }
                            .testTag("tag_entry_$tagId")
                    ) {
                        Text(
                            text = "#$tagId $name",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = onInputChange,
                    placeholder = { Text("Enter tag ID e.g. 1001", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmitCustom() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_tag_input"),
                    colors = OutlinedTextFieldDefaults.colors()
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSubmitCustom,
                    modifier = Modifier.testTag("btn_submit_tag_scan")
                ) {
                    Text("Scan Tag", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ProductDashboardCard(
    product: ProductEntity,
    activeTagMapping: com.example.data.RfidMappingEntity?,
    onRefreshAi: () -> Unit,
    onUpdateProductClick: () -> Unit,
    isAiLoading: Boolean,
    aiAnalysis: com.example.ai.AiProductAnalysis?
) {
    val ecoScore = product.ecoScore
    val (calcDecision, calcGrade, calcRec) = com.example.ai.GeminiEcoAssistant.computeEcoDecision(ecoScore)
    val decision = aiAnalysis?.decision ?: calcDecision
    val grade = aiAnalysis?.grade ?: calcGrade
    val decisionRec = aiAnalysis?.decisionRecommendation ?: calcRec

    val decisionColor = when (decision) {
        "GREEN" -> EcoBadgeGood
        "YELLOW" -> EcoBadgeWarning
        else -> EcoBadgeBad
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // 1. Main Header Card with Decision Badge & Eco Score
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("product_dashboard_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = product.category.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = onUpdateProductClick,
                                    modifier = Modifier.testTag("btn_edit_product")
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Update", fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = product.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "RFID TAG UID: ${product.id}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )

                            if (activeTagMapping != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "Zone: ${activeTagMapping.zoneName}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Eco Gauge Canvas
                        EcoGaugeCanvas(
                            score = product.ecoScore,
                            size = 110.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Environmental Decision & Grade Classification Banner (Display Data Only)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = decisionColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, decisionColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().testTag("environmental_decision_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = decisionColor
                                ) {
                                    Text(
                                        text = decision, // GREEN, YELLOW, or RED
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "DECISION: $decisionRec",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = decisionColor
                                    )
                                    Text(
                                        text = "Environmental Rating & Classification",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, decisionColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "GRADE $grade",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = decisionColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metric Badges (Carbon, Water, Recycling)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            icon = Icons.Default.Co2,
                            label = "Carbon",
                            value = product.carbon,
                            color = if (product.ecoScore >= 60) EcoBadgeGood else EcoBadgeBad,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            icon = Icons.Default.WaterDrop,
                            label = "Water",
                            value = product.water,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            icon = Icons.Default.Recycling,
                            label = "Recycling",
                            value = product.recycling,
                            color = if (product.ecoScore >= 60) EcoBadgeGood else EcoBadgeWarning,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Environmental Impact Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (product.ecoScore >= 60) EcoBadgeGood.copy(alpha = 0.1f)
                                else EcoBadgeBad.copy(alpha = 0.1f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (product.ecoScore >= 60) EcoBadgeGood.copy(alpha = 0.3f) else EcoBadgeBad.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = if (product.ecoScore >= 60) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (product.ecoScore >= 60) EcoBadgeGood else EcoBadgeBad,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (product.ecoScore >= 60) "Low Environmental Footprint" else "High Environmental Burden Warning",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (product.ecoScore >= 60) EcoBadgeGood else EcoBadgeBad
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = product.impact,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footprint Comparison Chart
                    FootprintComparisonChart(
                        productName = product.name,
                        carbonText = product.carbon,
                        waterText = product.water,
                        ecoScore = product.ecoScore,
                        alternativeName = product.alternative
                    )
                }
            }

            // 2. Greener Alternative Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Better Alternative Recommendation",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = product.alternative,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 3. Why This Score? Breakdown Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Why This Score?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = aiAnalysis?.whyThisScore ?: "Score calculated from embodied emissions (${product.carbon}), lifecycle water footprint (${product.water}), and circular material stream (${product.recycling}).",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Key Impact Drivers
                    Text(
                        text = "Key Impact Drivers",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val drivers = aiAnalysis?.keyImpactDrivers ?: listOf(
                        "Embodied Carbon Footprint: ${product.carbon}",
                        "Lifecycle Water Consumption: ${product.water}",
                        "Post-Consumer Fate: ${product.recycling}"
                    )
                    drivers.forEach { driver ->
                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(5.dp).background(decisionColor, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(driver, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Positive Factors
                    Text(
                        text = "Positive Factors",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoBadgeGood
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val positives = aiAnalysis?.positiveFactors ?: listOf(
                        "Standardized recyclability profile (${product.recycling})",
                        "Identified lower-impact alternative available"
                    )
                    positives.forEach { pos ->
                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EcoBadgeGood, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(pos, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                        }
                    }
                }
            }

            // 4. Gemini AI Insights & Sustainability Advice
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini AI Lifecycle Insight",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = onRefreshAi) {
                            if (isAiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (aiAnalysis != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AiPointItem(
                                title = "Environmental Impact",
                                desc = aiAnalysis.summary,
                                iconColor = MaterialTheme.colorScheme.primary
                            )
                            AiPointItem(
                                title = "Greener Suggestion",
                                desc = aiAnalysis.greenerAdvice,
                                iconColor = EcoBadgeGood
                            )
                            AiPointItem(
                                title = "Personalized Habit Tip",
                                desc = aiAnalysis.habitTip,
                                iconColor = EcoBadgeWarning
                            )
                            AiPointItem(
                                title = "Disposal Guidance",
                                desc = aiAnalysis.disposalGuidance,
                                iconColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else if (isAiLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyzing product sustainability data with AI...", fontSize = 11.sp)
                        }
                    }
                }
            }

            // 5. Circular Economy 6R Framework Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Recycling,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Circular Economy Action Matrix (6R)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val r6Map = aiAnalysis?.circularEconomyR6 ?: mapOf(
                        "USE BETTER" to "Select low-emission alternatives verified for durability.",
                        "REUSE" to "Maximize continuous usage cycles before disposal.",
                        "REPAIR" to "Maintain product elements to prolong functional lifespan.",
                        "REDUCE" to "Curtail single-use procurement to conserve raw virgin materials.",
                        "RECYCLE" to "Segregate cleanly into ${product.recycling} streams.",
                        "REPLACE" to "Transition permanently to ${product.alternative}."
                    )

                    r6Map.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (action, desc) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = action,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = desc,
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 6. Data & Architecture Status (Explicit Unidirectional Telemetry)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EcoBadgeGood,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data Status: ${aiAnalysis?.dataStatus ?: "Deterministic LCA Verified • Telemetry: Arduino -> App Only"}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun AiPointItem(
    title: String,
    desc: String,
    iconColor: Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(iconColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = iconColor)
            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun BackendDatabaseSyncCard(
    isSyncing: Boolean,
    syncStatus: String?,
    onSyncFullDatabase: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backend_db_sync_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Backend Database Synchronization",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onSyncFullDatabase,
                    enabled = !isSyncing,
                    modifier = Modifier.testTag("btn_sync_full_db")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Syncing...", fontSize = 11.sp)
                    } else {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync DB Now", fontSize = 11.sp)
                    }
                }
            }

            if (!syncStatus.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = syncStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyScanPlaceholder() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Scan an RFID Product Tag", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any preset tag above or swipe your RFID tag near the RC522 Arduino Reader to view environmental metrics.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun HeroAestheticBanner(
    roomRecordCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- TOP HERO CARD ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_aesthetic_banner")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF16261F),
                                Color(0xFF1E3A2F),
                                Color(0xFF0B1510)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "🌿 Eco Mind • Active Network",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Good Evening Joel",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "IoT Sustainability Hub • Realtime Environmental Insights",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Animated Progress Ring for Eco Score
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(72.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { 0.88f },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 7.dp,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "88",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "ECO SCORE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "14.2 kg", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "Carbon Saved", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline))

                        Column {
                            Text(text = "Arduino R4", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Hardware", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline))

                        Column {
                            Text(text = "$roomRecordCount Logs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Room DB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // --- SECOND ROW: TWO CARDS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Environmental Score Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_env_score")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EcoBadgeGood.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Grade A+",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoBadgeGood,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "88 / 100",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Environmental Score",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+12% vs last week",
                        fontSize = 10.sp,
                        color = EcoBadgeGood,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Carbon Footprint Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_carbon_footprint")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Co2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EcoBadgeGood.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "-0.8 kg",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoBadgeGood,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "2.4 kg",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Carbon Footprint",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Optimal low footprint",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- THIRD ROW: 4 STATUS INDICATORS ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_hardware_status_indicators")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicator 1: Bluetooth
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EcoBadgeGood)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Bluetooth", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Connected", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Indicator 2: Arduino
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EcoBadgeGood)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Arduino", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Active", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Indicator 3: RFID Reader
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EcoBadgeGood)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "RFID Reader", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Ready", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Indicator 4: Database Sync
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EcoBadgeGood)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Cloud Sync", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Synced", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun GeminiSustainabilityAdviceCard(
    advice: com.example.ai.EnvironmentalSustainabilityAdvice?,
    isLoading: Boolean,
    onRefreshAdvice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_gemini_sustainability_advice")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Environmental Sustainability Suggestions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Firestore Sensor Data Intelligence",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Firestore Sync",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Analyzing Firestore environmental telemetry...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (advice != null) {
                // Air Quality Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusBgColor = when {
                        advice.airQualityRating.contains("Optimal", ignoreCase = true) || advice.airQualityRating.contains("Good", ignoreCase = true) -> EcoBadgeGood
                        advice.airQualityRating.contains("Alert", ignoreCase = true) || advice.airQualityRating.contains("High", ignoreCase = true) -> EcoBadgeBad
                        else -> EcoBadgeWarning
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusBgColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Thermostat,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = advice.airQualityRating,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = "Updated just now",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Summary
                Text(
                    text = advice.summary,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Actionable Sustainability Steps:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                advice.actionableSuggestions.forEach { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = suggestion,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Energy Saving Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "HVAC & Energy Efficiency Tip",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = advice.energySavingTip,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Refresh Button
                OutlinedButton(
                    onClick = onRefreshAdvice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_refresh_gemini_sustainability_advice")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Re-analyze Firestore Environmental Telemetry",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onRefreshAdvice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate AI Sustainability Suggestions")
                }
            }
        }
    }
}


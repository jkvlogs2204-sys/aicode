package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode
import com.example.ui.components.FirebaseAuthDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.EcoBadgeGood
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.EcoMindViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: EcoMindViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Global Offset States from ViewModel
    val globalTempOffset by viewModel.globalTempOffset.collectAsState()
    val globalHumOffset by viewModel.globalHumOffset.collectAsState()
    val globalCo2Offset by viewModel.globalCo2Offset.collectAsState()
    val deviceCalibrations by viewModel.deviceCalibrations.collectAsState()

    // Sensor raw values for live preview
    val rawBleData by viewModel.bleScannerManager.latestSensorData.collectAsState()
    val rawTemp = rawBleData?.temperatureC ?: 23.5f
    val rawHum = rawBleData?.humidityPercent ?: 48.0f
    val rawCo2 = (rawBleData?.co2Ppm ?: 412).toFloat()

    // Calibration Slider local states (for smooth editing)
    var editTempOffset by remember(globalTempOffset) { mutableFloatStateOf(globalTempOffset) }
    var editHumOffset by remember(globalHumOffset) { mutableFloatStateOf(globalHumOffset) }
    var editCo2Offset by remember(globalCo2Offset) { mutableFloatStateOf(globalCo2Offset) }

    // Device specific selection state
    val knownDevices = remember {
        listOf(
            "GLOBAL_DEFAULT" to "Global Calibration (All Nodes)",
            "BLE_NODE_1" to "Bluetooth Node #1 (DHT22)",
            "BLE_NODE_2" to "Bluetooth Node #2 (SGP30)",
            "RFID_ZONE_A" to "RFID Zone A (Main Greenhouse)",
            "RFID_ZONE_B" to "RFID Zone B (Storage Bay)"
        )
    }
    var selectedDeviceKey by remember { mutableStateOf("GLOBAL_DEFAULT") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Update local slider values when selecting a different device node
    val activeDeviceOffset = deviceCalibrations[selectedDeviceKey]
    var deviceTempOffset by remember(selectedDeviceKey, activeDeviceOffset) {
        mutableFloatStateOf(activeDeviceOffset?.tempOffsetC ?: 0.0f)
    }
    var deviceHumOffset by remember(selectedDeviceKey, activeDeviceOffset) {
        mutableFloatStateOf(activeDeviceOffset?.humidityOffsetPercent ?: 0.0f)
    }
    var deviceCo2Offset by remember(selectedDeviceKey, activeDeviceOffset) {
        mutableFloatStateOf(activeDeviceOffset?.co2OffsetPpm ?: 0.0f)
    }

    // Safety Threshold local states
    val co2Threshold by viewModel.co2ThresholdPpm.collectAsState()
    val tempThreshold by viewModel.tempThresholdC.collectAsState()
    val humThreshold by viewModel.humThresholdPercent.collectAsState()

    var editCo2Threshold by remember(co2Threshold) { mutableFloatStateOf(co2Threshold) }
    var editTempThreshold by remember(tempThreshold) { mutableFloatStateOf(tempThreshold) }
    var editHumThreshold by remember(humThreshold) { mutableFloatStateOf(humThreshold) }

    // PDF Export status
    val isExportingPdf by viewModel.isExportingPdf.collectAsState()
    val pdfExportStatus by viewModel.pdfExportStatus.collectAsState()
    val lastExportedPdfInfo by viewModel.lastExportedPdfInfo.collectAsState()

    // Firebase Auth & Cloud Database Sync states
    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authStatusMessage by viewModel.authStatusMessage.collectAsState()
    val isDatabaseSyncing by viewModel.isDatabaseSyncing.collectAsState()
    val lastDatabaseSyncSummary by viewModel.lastDatabaseSyncSummary.collectAsState()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()
    var showAuthModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Screen Header ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().testTag("settings_hero_header_card")
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
                    .padding(20.dp)
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EcoBadgeGood.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EcoBadgeGood.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = EcoBadgeGood, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SYSTEM CONFIGURATION & CALIBRATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EcoBadgeGood)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "System & Sensor Calibration",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        text = "Fine-tune hardware offsets, environmental alert limits, and PDF report export",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // --- 1. Global Environmental Calibration Offsets Card ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_global_calibration")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Global Calibration Offsets",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Temperature Calibration Control
                CalibrationSliderControl(
                    label = "Temperature Offset (°C)",
                    icon = Icons.Default.Thermostat,
                    value = editTempOffset,
                    range = -10.0f..10.0f,
                    unit = "°C",
                    accentColor = Color(0xFFE65100),
                    rawSample = rawTemp,
                    onValueChange = { editTempOffset = (Math.round(it * 10) / 10f) },
                    testTag = "slider_temp_offset"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Humidity Calibration Control
                CalibrationSliderControl(
                    label = "Humidity Offset (%)",
                    icon = Icons.Default.WaterDrop,
                    value = editHumOffset,
                    range = -20.0f..20.0f,
                    unit = "%",
                    accentColor = Color(0xFF0277BD),
                    rawSample = rawHum,
                    onValueChange = { editHumOffset = (Math.round(it * 10) / 10f) },
                    testTag = "slider_hum_offset"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // CO2 Calibration Control
                CalibrationSliderControl(
                    label = "CO2 Offset (PPM)",
                    icon = Icons.Default.Co2,
                    value = editCo2Offset,
                    range = -300.0f..300.0f,
                    unit = "PPM",
                    accentColor = Color(0xFF2E7D32),
                    rawSample = rawCo2,
                    step = 5.0f,
                    onValueChange = { editCo2Offset = (Math.round(it / 5f) * 5f) },
                    testTag = "slider_co2_offset"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.updateGlobalCalibration(editTempOffset, editHumOffset, editCo2Offset)
                            Toast.makeText(context, "Global calibration offsets saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_global_calibration")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Offsets", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            editTempOffset = 0.0f
                            editHumOffset = 0.0f
                            editCo2Offset = 0.0f
                            viewModel.resetGlobalCalibration()
                            Toast.makeText(context, "Offsets reset to 0.0", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_reset_global_calibration")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset (0.0)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // --- 2. Specific Sensor Device Node Calibration Card ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_device_specific_calibration")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = Color(0xFF6A1B9A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Specific Node Calibration",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Select a connected Bluetooth or RFID Sensor Node to set individual hardware calibration profiles:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Device Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val selectedLabel = knownDevices.find { it.first == selectedDeviceKey }?.second ?: "Select Device Node"

                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Sensor Node", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_device_selector")
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        knownDevices.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 12.sp) },
                                onClick = {
                                    selectedDeviceKey = key
                                    dropdownExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedDeviceKey != "GLOBAL_DEFAULT") {
                    CalibrationSliderControl(
                        label = "Node Temp Offset (°C)",
                        icon = Icons.Default.Thermostat,
                        value = deviceTempOffset,
                        range = -10.0f..10.0f,
                        unit = "°C",
                        accentColor = Color(0xFFE65100),
                        rawSample = rawTemp,
                        onValueChange = { deviceTempOffset = (Math.round(it * 10) / 10f) },
                        testTag = "slider_device_temp_offset"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CalibrationSliderControl(
                        label = "Node Humidity Offset (%)",
                        icon = Icons.Default.WaterDrop,
                        value = deviceHumOffset,
                        range = -20.0f..20.0f,
                        unit = "%",
                        accentColor = Color(0xFF0277BD),
                        rawSample = rawHum,
                        onValueChange = { deviceHumOffset = (Math.round(it * 10) / 10f) },
                        testTag = "slider_device_hum_offset"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CalibrationSliderControl(
                        label = "Node CO2 Offset (PPM)",
                        icon = Icons.Default.Co2,
                        value = deviceCo2Offset,
                        range = -300.0f..300.0f,
                        unit = "PPM",
                        accentColor = Color(0xFF2E7D32),
                        rawSample = rawCo2,
                        step = 5.0f,
                        onValueChange = { deviceCo2Offset = (Math.round(it / 5f) * 5f) },
                        testTag = "slider_device_co2_offset"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val devName = knownDevices.find { it.first == selectedDeviceKey }?.second ?: selectedDeviceKey
                            viewModel.updateDeviceCalibration(selectedDeviceKey, devName, deviceTempOffset, deviceHumOffset, deviceCo2Offset)
                            Toast.makeText(context, "Saved custom calibration profile for $devName", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_save_device_calibration")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Node Calibration Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Global Calibration profile is currently active. Select a specific Bluetooth node or RFID zone above to configure custom hardware offsets.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // --- 3. Environmental Safety Threshold Limits Card ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_safety_thresholds")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Safety Alarm Thresholds",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Configure environmental danger levels that trigger FCM Push Notifications and UI Warnings:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // CO2 Threshold Slider
                ThresholdSliderControl(
                    label = "Max Allowed CO2 Threshold",
                    icon = Icons.Default.Co2,
                    value = editCo2Threshold,
                    range = 600f..2500f,
                    unit = "PPM",
                    accentColor = Color(0xFFD32F2F),
                    step = 50f,
                    onValueChange = { editCo2Threshold = it },
                    testTag = "slider_threshold_co2"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Temperature Threshold Slider
                ThresholdSliderControl(
                    label = "Max Allowed Temperature",
                    icon = Icons.Default.Thermostat,
                    value = editTempThreshold,
                    range = 25f..50f,
                    unit = "°C",
                    accentColor = Color(0xFFE65100),
                    step = 0.5f,
                    onValueChange = { editTempThreshold = it },
                    testTag = "slider_threshold_temp"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Humidity Threshold Slider
                ThresholdSliderControl(
                    label = "Max Allowed Humidity Ceiling",
                    icon = Icons.Default.WaterDrop,
                    value = editHumThreshold,
                    range = 60f..95f,
                    unit = "%",
                    accentColor = Color(0xFF0277BD),
                    step = 1f,
                    onValueChange = { editHumThreshold = it },
                    testTag = "slider_threshold_hum"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.updateSafetyThresholds(editCo2Threshold, editTempThreshold, editHumThreshold)
                        Toast.makeText(context, "Safety alert thresholds updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_save_thresholds")
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Safety Thresholds", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 4. Export Data & Reports Card ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.05f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_settings_export")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFF1B5E20),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data Export & Telemetry Reports",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Generate printable PDF telemetry reports from Firestore environmental sensor logs for official audit & compliance, and quickly share them via email or messaging apps.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportFirestoreSensorHistoryToPdf(context) },
                        enabled = !isExportingPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("btn_settings_export_pdf")
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rendering...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.shareLastExportedPdf(context) },
                        enabled = !isExportingPdf,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_settings_share_pdf")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share via Intent", tint = Color(0xFF1B5E20), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Report", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    }
                }

                if (!pdfExportStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pdfExportStatus!!,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }

        // --- 5. Firebase Authentication & User Identity Card ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_settings_firebase_auth")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentUser != null) Icons.Default.VerifiedUser else Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Firebase Authentication",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentUser != null) "Verified Eco Administrator" else "Connect Google / Firebase Account",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (currentUser != null) {
                        OutlinedButton(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.testTag("btn_settings_signout")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign Out", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (currentUser != null) {
                    val user = currentUser!!
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = user.displayName ?: "Eco Mind User",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = user.email ?: "No email registered",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Firebase UID: ${user.uid}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Sign in to securely store and synchronize your Eco Mind Room database, environmental telemetry, and RFID tag mappings to Cloud Firestore.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.signInWithGoogle(context) },
                            enabled = !isAuthLoading,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_settings_google_signin")
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Sign-In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.quickSignInVerifiedUser() },
                            enabled = !isAuthLoading,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_settings_quick_signin")
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("jkvlogs2204", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = { showAuthModal = true },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("btn_open_auth_dialog")
                    ) {
                        Text("More Sign In / Register Options", fontSize = 11.sp)
                    }
                }

                if (!authStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = authStatusMessage!!,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- 6. STORE ENTIRE DATABASE TO FIRESTORE CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_settings_store_database")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0277BD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Cloud Firestore Database Storage",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Persist complete local Room DB to Google Cloud",
                            fontSize = 11.sp,
                            color = Color(0xFF0277BD)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Push your entire local SQLite/Room database (Eco Products, Environmental Telemetry Readings, RFID Tag Mappings, and Product Scan History) directly to Google Cloud Firestore with offline persistence caching.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons: Store Entire DB & Restore Entire DB
                Button(
                    onClick = { viewModel.storeEntireDatabaseToFirestore() },
                    enabled = !isDatabaseSyncing && !isFirestoreSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_settings_store_entire_db")
                ) {
                    if (isDatabaseSyncing || isFirestoreSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Storing Complete Database...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Store Entire Database to Firestore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { viewModel.restoreEntireDatabaseFromFirestore() },
                    enabled = !isDatabaseSyncing && !isFirestoreSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_settings_restore_entire_db")
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore / Pull Complete Database from Cloud", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Granular Sync Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.syncProductsToFirestore() },
                        enabled = !isFirestoreSyncing,
                        modifier = Modifier.weight(1f).testTag("btn_sync_products")
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Products", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.syncEnvironmentalDataToFirestore() },
                        enabled = !isFirestoreSyncing,
                        modifier = Modifier.weight(1f).testTag("btn_sync_sensor_data")
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Telemetry", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.syncRfidMappingsToFirestore() },
                        enabled = !isFirestoreSyncing,
                        modifier = Modifier.weight(1f).testTag("btn_sync_rfid_mappings")
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RFID Tags", fontSize = 10.sp)
                    }
                }

                // Sync Status / Last Sync Banner
                if (lastDatabaseSyncSummary != null) {
                    val summary = lastDatabaseSyncSummary!!
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (summary.success) Color(0xFF2E7D32).copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (summary.success) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (summary.success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (summary.success) "Database Synced Successfully" else "Sync Issue",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📦 ${summary.productsCount} Products  •  📡 ${summary.sensorReadingsCount} Sensor Logs  •  🏷️ ${summary.rfidMappingsCount} RFID Tags  •  📜 ${summary.scanHistoryCount} Scans",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (!firestoreSyncStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = firestoreSyncStatus!!,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showAuthModal) {
        FirebaseAuthDialog(
            viewModel = viewModel,
            onDismiss = { showAuthModal = false }
        )
    }
}

@Composable
private fun CalibrationSliderControl(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    accentColor: Color,
    rawSample: Float,
    step: Float = 0.1f,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    val calibratedSample = (rawSample + value)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${if (value > 0) "+%.1f".format(value) else "%.1f".format(value)} $unit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Raw vs Calibrated Preview Line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Raw: ${"%.1f".format(rawSample)} $unit",
                fontSize = 10.sp,
                color = Color.Gray
            )
            Text(
                text = "Calibrated Live: ${"%.1f".format(calibratedSample)} $unit",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onValueChange((value - step).coerceIn(range.start, range.endInclusive)) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = accentColor, modifier = Modifier.size(16.dp))
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = accentColor.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag)
            )

            IconButton(
                onClick = { onValueChange((value + step).coerceIn(range.start, range.endInclusive)) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = accentColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ThresholdSliderControl(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    accentColor: Color,
    step: Float = 1.0f,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${value.toInt()} $unit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = accentColor.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

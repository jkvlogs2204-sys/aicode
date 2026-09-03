package com.example.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.BluetoothState
import com.example.bluetooth.ble.BleDevice
import com.example.bluetooth.ble.BleGattState
import com.example.bluetooth.ble.BleScanState
import com.example.ui.EcoMindViewModel
import com.example.ui.theme.BioGreenLight
import com.example.ui.theme.EcoBadgeGood
import com.example.ui.theme.EcoBadgeWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HardwareSetupScreen(
    viewModel: EcoMindViewModel,
    modifier: Modifier = Modifier,
    bleScanningViewModel: com.example.ui.BleScanningViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val bluetoothState by viewModel.bluetoothState.collectAsState()
    val isClassicDiscovering by viewModel.isClassicDiscovering.collectAsState()
    val discoveredClassicDevices by viewModel.discoveredClassicDevices.collectAsState()
    val backendUrl by viewModel.backendUrl.collectAsState()
    val isBackendSyncing by viewModel.isBackendSyncing.collectAsState()
    val backendSyncStatus by viewModel.backendSyncStatus.collectAsState()
    val connectionPingResult by viewModel.connectionPingResult.collectAsState()
    val isMobileMasterNode by viewModel.isMobileMasterNode.collectAsState()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()
    val isFirestoreOfflineMode by viewModel.isFirestoreOfflineMode.collectAsState()
    val pendingOfflineWritesCount by viewModel.pendingOfflineWritesCount.collectAsState()
    val isExportingPdf by viewModel.isExportingPdf.collectAsState()
    val pdfExportStatus by viewModel.pdfExportStatus.collectAsState()
    val fcmToken by viewModel.fcmRegistrationToken.collectAsState()
    val lastPushNotificationAlert by viewModel.lastPushNotificationAlert.collectAsState()
    val criticalAlertCount by viewModel.criticalAlertCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDatabaseSyncing by viewModel.isDatabaseSyncing.collectAsState()
    val lastDatabaseSyncSummary by viewModel.lastDatabaseSyncSummary.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val bleScanState by viewModel.bleScanState.collectAsState()
    val bleDiscoveredDevices by viewModel.bleDiscoveredDevices.collectAsState()
    val bleGattState by viewModel.bleGattState.collectAsState()
    val bleSensorData by viewModel.bleLatestSensorData.collectAsState()

    var urlInput by remember(backendUrl) { mutableStateOf(backendUrl) }
    var copiedCode by remember { mutableStateOf(false) }
    var copiedServerCode by remember { mutableStateOf(false) }
    var filterCategory by remember { mutableStateOf("All") }
    val lastRawData by viewModel.lastRawData.collectAsState()
    var manualMacInput by remember { mutableStateOf("") }
    var showManualMacInput by remember { mutableStateOf(false) }
    var showWiringGuide by remember { mutableStateOf(false) }
    var showBleHc05ScannerDialog by remember { mutableStateOf(false) }
    val savedHc05 = remember(bluetoothState) { viewModel.getSavedHc05Device() }

    val pairedDevices = remember(bluetoothState) {
        viewModel.bluetoothManager.getPairedDevices()
    }

    val filteredBleDevices = remember(bleDiscoveredDevices, filterCategory) {
        when (filterCategory) {
            "Near" -> bleDiscoveredDevices.filter { it.rssi >= -75 }
            "Immediate" -> bleDiscoveredDevices.filter { it.rssi >= -55 }
            "Environmental" -> bleDiscoveredDevices.filter { it.isEnvironmentalSensor }
            else -> bleDiscoveredDevices
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Aesthetic Header Banner
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hardware_hero_header_card")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
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
                            Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = EcoBadgeGood, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("IOT HARDWARE & CLOUD GATEWAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EcoBadgeGood)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Hardware Nodes & Sensor Gateway",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        text = "Connect ESP32 / Arduino Nodes via BLE & Bluetooth Classic to Cloud Firestore",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = EcoBadgeGood, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("BLE Scanner", fontSize = 10.sp, color = Color.Gray)
                                    Text(if (bleScanState is BleScanState.Scanning) "Scanning..." else "Idle", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = EcoBadgeGood, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Firestore", fontSize = 10.sp, color = Color.Gray)
                                    Text(if (isFirestoreSyncing) "Syncing..." else "Connected", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Classic Bluetooth SPP (HC-05 Module Gateway)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("bluetooth_connection_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header with Badge & Scan Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "HC-05 Bluetooth Module",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "SPP 9600",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Arduino Wireless RFID & Telemetry Link",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (bluetoothState is BluetoothState.Connected) {
                            Button(
                                onClick = { viewModel.disconnectDevice() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_disconnect_hc05_hardware")
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Disconnect", fontSize = 11.sp)
                            }
                        } else {
                            if (isClassicDiscovering) {
                                Button(
                                    onClick = { viewModel.stopClassicDiscovery() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop", fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.startClassicDiscovery() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_scan_classic")
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Connection State Banner
                when (val state = bluetoothState) {
                    is BluetoothState.Connected -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EcoBadgeGood.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EcoBadgeGood.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EcoBadgeGood, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Connected: ${state.deviceName}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EcoBadgeGood
                                        )
                                        Text(
                                            text = "MAC: ${state.deviceAddress} • RFCOMM SPP Channel 1",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (lastRawData.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Serial Stream:",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = lastRawData,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is BluetoothState.Connecting -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EcoBadgeWarning.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EcoBadgeWarning)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Connecting to ${state.deviceName} via SPP...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoBadgeWarning
                                )
                            }
                        }
                    }
                    is BluetoothState.Reconnecting -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EcoBadgeWarning.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EcoBadgeWarning)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Reconnecting to ${state.deviceName} (Attempt ${state.attempt}/${state.maxAttempts})...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoBadgeWarning
                                )
                            }
                        }
                    }
                    is BluetoothState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.message,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { viewModel.retryBluetoothConnection() },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_retry_bt_connection")
                                ) {
                                    Text("Retry", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Offline • Tap your paired HC-05 below to establish direct link.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Quick One-Tap Reconnect to Saved HC-05
                if (savedHc05 != null && bluetoothState !is BluetoothState.Connected && bluetoothState !is BluetoothState.Connecting) {
                    val (savedMac, savedName) = savedHc05
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.autoConnectSavedHc05() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Saved HC-05: $savedName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(savedMac, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Button(
                                onClick = { viewModel.autoConnectSavedHc05() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Connect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Scanning Progress
                if (isClassicDiscovering) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                    )
                }

                // Direct MAC Input Toggle & Field
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showManualMacInput) "Hide MAC Input" else "Direct Connect by MAC Address",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showManualMacInput = !showManualMacInput }
                            .padding(vertical = 4.dp)
                    )

                    Text(
                        text = if (showWiringGuide) "Hide Wiring Guide" else "HC-05 Wiring & PIN Guide",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { showWiringGuide = !showWiringGuide }
                            .padding(vertical = 4.dp)
                    )
                }

                if (showManualMacInput) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualMacInput,
                            onValueChange = { manualMacInput = it },
                            placeholder = { Text("e.g. 98:D3:31:FC:12:34", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_hardware_manual_mac")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualMacInput.isNotBlank()) {
                                    viewModel.connectByMacAddress(manualMacInput.trim())
                                }
                            },
                            enabled = manualMacInput.length >= 11,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_connect_hardware_manual_mac")
                        ) {
                            Text("Connect", fontSize = 11.sp)
                        }
                    }
                }

                if (showWiringGuide) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("HC-05 Quick Wiring & Pairing:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Default Pairing PIN: 1234 or 0000", fontSize = 10.sp)
                            Text("• Arduino Connections: HC-05 VCC -> 5V, GND -> GND, TX -> Pin 2 (RX), RX -> Pin 3 (TX via divider)", fontSize = 10.sp)
                            Text("• Baud Rate: 9600 bps", fontSize = 10.sp)
                            Text("• LED Indicators: Fast blink (2 Hz) = Waiting to pair; 2 quick flashes = Connected.", fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Discovered Devices
                if (discoveredClassicDevices.isNotEmpty()) {
                    Text(text = "Discovered Nearby Devices (${discoveredClassicDevices.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    discoveredClassicDevices.forEach { device ->
                        PairedDeviceRow(
                            device = device,
                            onConnect = { viewModel.connectDevice(device) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Paired Devices
                if (pairedDevices.isNotEmpty()) {
                    Text(text = "Paired Devices in Phone (${pairedDevices.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    pairedDevices.forEach { device ->
                        PairedDeviceRow(
                            device = device,
                            onConnect = { viewModel.connectDevice(device) }
                        )
                    }
                } else if (discoveredClassicDevices.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No paired Classic Bluetooth devices found. Pair your HC-05 in Android Settings > Bluetooth (PIN: 1234 or 0000), or tap 'Scan' above.",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // 2. BLE Utility Section Header & Scanner Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().testTag("ble_scanner_utility_card")
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Bluetooth Low Energy (BLE)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Arduino Environmental Sensors Utility", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    if (bleScanState is BleScanState.Scanning) {
                        Button(
                            onClick = { viewModel.stopBleScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("stop_ble_scan_button")
                        ) {
                            Text("Stop", fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startBleScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("start_ble_scan_button")
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan BLE", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Launch Full BLE & HC-05 IoT Scanner Dialog
                Button(
                    onClick = { showBleHc05ScannerDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_open_ble_hc05_scanner_dialog")
                ) {
                    Icon(imageVector = Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Launch BLE & HC-05 IoT Scanner & Live Telemetry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scan Status Bar
                when (bleScanState) {
                    is BleScanState.Scanning -> {
                        val count = (bleScanState as BleScanState.Scanning).discoveredCount
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EcoBadgeWarning))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scanning for nearby BLE sensor nodes...", fontSize = 11.sp, color = EcoBadgeWarning, fontWeight = FontWeight.Medium)
                                }
                                Text("Found $count", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is BleScanState.Stopped -> {
                        val total = (bleScanState as BleScanState.Stopped).totalFound
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EcoBadgeGood))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Completed. Discovered $total BLE peripherals.", fontSize = 11.sp, color = EcoBadgeGood, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    is BleScanState.Error -> {
                        Text("Scan Error: ${(bleScanState as BleScanState.Error).message}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {
                        Text("Tap 'Scan BLE' to discover nearby Arduino, ESP32, and HM-10 sensor modules.", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("All", "Immediate", "Near", "Environmental").forEach { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = { filterCategory = cat },
                            label = { Text(cat, fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Discovered Devices List
                if (filteredBleDevices.isNotEmpty()) {
                    Text("Discovered BLE Peripherals (${filteredBleDevices.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    filteredBleDevices.forEach { bleDevice ->
                        BleDeviceItemCard(
                            device = bleDevice,
                            isConnected = (bleGattState is BleGattState.Connected && (bleGattState as BleGattState.Connected).device.address == bleDevice.address),
                            onConnect = { viewModel.connectBleDevice(bleDevice) }
                        )
                    }
                } else if (bleScanState is BleScanState.Idle) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "BLE Utility Ready: Supports Arduino Nano 33 BLE, ESP32-S3, HM-10, Adafruit Feather, and Nordic UART GATT environmental nodes.",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // 2. Active BLE GATT Connected Monitor (If connected)
        if (bleGattState is BleGattState.Connected) {
            val connectedState = bleGattState as BleGattState.Connected
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("ble_gatt_active_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = connectedState.device.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "MAC: ${connectedState.device.address} | RSSI: ${connectedState.device.rssi} dBm",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.disconnectBleGatt() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("disconnect_ble_button")
                        ) {
                            Text("Disconnect GATT", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Telemetry Stream Grid
                    Text("Live BLE Telemetry & Sensor Stream:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Temperature Gauge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Thermostat, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Temp", fontSize = 10.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${bleSensorData?.temperatureC ?: "--"} °C",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Humidity Gauge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Humidity", fontSize = 10.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${bleSensorData?.humidityPercent ?: "--"} %",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Air Quality / CO2 Gauge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Co2, contentDescription = null, tint = EcoBadgeGood, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CO₂ Level", fontSize = 10.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${bleSensorData?.co2Ppm ?: "--"} ppm",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sensor Commands & RFID Transmit
                    Text("Transmit Scanned Tag ID via BLE:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("1001", "1002", "1003", "1004").forEach { tagId ->
                            OutlinedButton(
                                onClick = { viewModel.triggerBleRfidScan(tagId) },
                                modifier = Modifier.weight(1f).testTag("transmit_ble_tag_$tagId")
                            ) {
                                Text("Tag $tagId", fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EcoBadgeGood,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Strictly Unidirectional Telemetry (Hardware → Mobile App)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else if (bleGattState is BleGattState.Connecting) {
            val connectingName = (bleGattState as BleGattState.Connecting).deviceName
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(EcoBadgeWarning))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Establishing GATT connection to $connectingName...", fontSize = 12.sp, color = EcoBadgeWarning, fontWeight = FontWeight.Bold)
                }
            }
        }



        // 4. System Architecture Flow
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "System Architecture & BLE Integration", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                val steps = listOf(
                    "1. RFID Tag Detection" to "MFRC522 detects RFID Tag UID and normalizes tag payload.",
                    "2. Unidirectional Serial / BLE Broadcast" to "Arduino transmits UID over HC-05 / BLE UART to Mobile App.",
                    "3. Backend & Eco Score Evaluation" to "App queries REST Backend & Gemini AI for lifecycle sustainability analysis.",
                    "4. Environmental Decision On-Screen" to "Mobile screen renders GREEN/YELLOW/RED decision, grade & 6R circular matrix (Zero reverse hardware commands)."
                )

                steps.forEach { (title, desc) ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = desc, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // 5. Hardware Pin Connections
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.DeveloperBoard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Arduino Uno & Sensor Wiring", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                val pins = listOf(
                    "HC-05 Bluetooth TX" to "Pin 2 (Arduino RX via divider)",
                    "HC-05 Bluetooth RX" to "Pin 3 (Arduino TX)",
                    "DHT22 Temp & Humidity" to "Pin 7 (Digital)",
                    "MQ-135 Air Quality Sensor" to "A0 (Analog)",
                    "RC522 RFID Reader" to "Pin 10 (SDA), Pin 9 (RST)",
                    "RC522 SPI Bus" to "Pin 13 (SCK), Pin 12 (MISO), Pin 11 (MOSI)"
                )

                pins.forEach { (component, pin) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = component, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = pin, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 6. Node.js REST API Server & Multi-Mobile Network Sync Configuration
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().testTag("card_backend_multimobile")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Multi-Mobile REST API & Network Backend", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Room DB + REST",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sync Room Database across multiple mobile devices via local Wi-Fi IP, Cloud REST server, or Mobile Master Host:",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Preset IP Selectors
                Text("Preset Network Hosts:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "Emulator" to "http://10.0.2.2:3000",
                        "Wi-Fi LAN" to "http://192.168.1.100:3000",
                        "Hotspot" to "http://192.168.43.1:3000",
                        "Cloud API" to "https://api.ecomind.app"
                    )
                    presets.forEach { (label, url) ->
                        FilterChip(
                            selected = urlInput.trim() == url,
                            onClick = {
                                urlInput = url
                                viewModel.setBackendUrl(url)
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.testTag("preset_host_$label")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input & Actions Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("e.g. http://192.168.1.50:3000") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("backend_url_input"),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.setBackendUrl(urlInput.trim()) },
                        modifier = Modifier.testTag("save_url_button")
                    ) {
                        Text("Save Host", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ping Test & Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.testBackendConnection() },
                        modifier = Modifier.testTag("btn_test_backend_ping")
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Connection Ping", fontSize = 11.sp)
                    }

                    if (!connectionPingResult.isNullOrBlank() && connectionPingResult != "Not Tested") {
                        val isSuccess = connectionPingResult!!.startsWith("SUCCESS")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSuccess) EcoBadgeGood.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (isSuccess) "HEALTHY (200 OK)" else "OFFLINE / UNREACHABLE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSuccess) EcoBadgeGood else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (!connectionPingResult.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = connectionPingResult!!,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Remote DB Pull and Push Operations Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.fetchFullDatabaseFromBackend() },
                        enabled = !isBackendSyncing,
                        modifier = Modifier.weight(1f).testTag("btn_pull_remote_db")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pull DB from Remote", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.syncFullDatabaseToBackend() },
                        enabled = !isBackendSyncing,
                        modifier = Modifier.weight(1f).testTag("btn_push_remote_db")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Push Room DB to Remote", fontSize = 11.sp)
                    }
                }

                if (!backendSyncStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = backendSyncStatus!!,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mobile Master Node Host Mode Section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Mobile Master Node Host Mode",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Allow other mobiles on Wi-Fi to sync with this device",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Switch(
                                checked = isMobileMasterNode,
                                onCheckedChange = { viewModel.toggleMobileMasterNode(it) },
                                modifier = Modifier.testTag("switch_mobile_master_node")
                            )
                        }

                        if (isMobileMasterNode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val connectionPayload = "$backendUrl/sync-database"
                            Text(
                                text = "Shareable Master Node Endpoint:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = connectionPayload,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(connectionPayload))
                                    },
                                    modifier = Modifier.testTag("btn_copy_master_payload")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Connection URL", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Node.js Server Code Copy Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Node.js Multi-Mobile Express Script (server.js):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(sampleNodeExpressServerCode))
                            copiedServerCode = true
                        },
                        modifier = Modifier.testTag("btn_copy_server_code")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (copiedServerCode) "Copied server.js!" else "Copy server.js", fontSize = 11.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sampleNodeExpressServerCodeSnippet,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Firebase Firestore Cloud Data Sync Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().testTag("card_firestore_sync")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Firebase Firestore Cloud Sync", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Room ↔ Firestore",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sync environmental readings and eco products between Room SQLite and Google Cloud Firestore for seamless multi-device access.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Offline Persistence Banner & Controls
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFirestoreOfflineMode) Color(0xFFE65100).copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFirestoreOfflineMode) "OFFLINE PERSISTENCE ACTIVE" else "ONLINE SYNC ACTIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFirestoreOfflineMode) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isFirestoreOfflineMode)
                                        "Telemetry logged to disk offline. Will auto-sync when online."
                                    else
                                        "Real-time cloud replication enabled. Offline disk cache ready.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = isFirestoreOfflineMode,
                                onCheckedChange = { viewModel.toggleFirestoreOfflineMode(it) },
                                modifier = Modifier.testTag("switch_firestore_offline_mode")
                            )
                        }

                        if (pendingOfflineWritesCount > 0 || isFirestoreOfflineMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE65100).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "$pendingOfflineWritesCount Pending Offline Writes Queued",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                if (!isFirestoreOfflineMode && pendingOfflineWritesCount > 0) {
                                    Button(
                                        onClick = { viewModel.flushOfflineFirestoreQueue() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.testTag("btn_flush_offline_cache")
                                    ) {
                                        Text("Auto-Sync Now", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // User Identity / Cloud Target Banner
                if (currentUser != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Authenticated as: ${currentUser?.email ?: "jkvlogs2204@gmail.com"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // PRIMARY ACTION: Store Entire Database to Cloud Firestore
                Button(
                    onClick = { viewModel.storeEntireDatabaseToFirestore() },
                    enabled = !isDatabaseSyncing && !isFirestoreSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_backup_entire_db_firestore")
                ) {
                    if (isDatabaseSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Storing Database to Cloud...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Store Entire Database to Firestore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { viewModel.restoreEntireDatabaseFromFirestore() },
                    enabled = !isDatabaseSyncing && !isFirestoreSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_restore_entire_db_firestore")
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Entire Database from Firestore", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.fetchEnvironmentalDataFromFirestore() },
                        enabled = !isFirestoreSyncing,
                        modifier = Modifier.weight(1f).testTag("btn_pull_firestore_env")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pull Env Logs", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.syncEnvironmentalDataToFirestore() },
                        enabled = !isFirestoreSyncing,
                        modifier = Modifier.weight(1f).testTag("btn_push_firestore_env")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Push Env Logs", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.fetchProductsFromFirestore() },
                        enabled = !isFirestoreSyncing,
                        modifier = Modifier.weight(1f).testTag("btn_pull_firestore_products")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pull Products", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.syncProductsToFirestore() },
                        enabled = !isFirestoreSyncing,
                        modifier = Modifier.weight(1f).testTag("btn_push_firestore_products")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Push Products", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.fetchRfidMappingsFromFirestore() },
                        enabled = !isFirestoreSyncing,
                        modifier = Modifier.weight(1f).testTag("btn_pull_firestore_rfid")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pull RFID Tags", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.syncRfidMappingsToFirestore() },
                        enabled = !isFirestoreSyncing,
                        modifier = Modifier.weight(1f).testTag("btn_push_firestore_rfid")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Push RFID Tags", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportFirestoreSensorHistoryToPdf(context) },
                        enabled = !isExportingPdf && !isFirestoreSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("btn_export_firestore_pdf")
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rendering...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.shareLastExportedPdf(context) },
                        enabled = !isExportingPdf && !isFirestoreSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_share_firestore_pdf")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Report", tint = Color(0xFF1B5E20), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Report", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    }
                }

                if (!pdfExportStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pdfExportStatus!!,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20)
                    )
                }

                if (!firestoreSyncStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = firestoreSyncStatus!!,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 7. Firebase Cloud Messaging (FCM) Push Notifications for Critical Sensor Thresholds
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.06f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_fcm_push_notifications")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "FCM Push Notifications",
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Firebase Push Notifications (FCM)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Critical Environmental Threshold Warnings",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD32F2F).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$criticalAlertCount ALERTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FCM Token Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FCM REGISTRATION TOKEN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = fcmToken ?: "Fetching token...",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = {
                                fcmToken?.let { token ->
                                    clipboardManager.setText(AnnotatedString(token))
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Copy FCM Token",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "CRITICAL SAFETY THRESHOLDS:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Co2, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                            Text("CO2 > 1000 PPM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Push Warning", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Thermostat, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                            Text("> 35°C or < 5°C", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Extreme Temp", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF0277BD), modifier = Modifier.size(14.dp))
                            Text("> 85% Humidity", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Mold Hazard", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (!lastPushNotificationAlert.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFD32F2F).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = lastPushNotificationAlert!!,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 7. Copy Arduino Sketch (.ino) Code
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Arduino BLE Sketch (.ino)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(sampleBleArduinoCode))
                            copiedCode = true
                        },
                        modifier = Modifier.testTag("copy_arduino_code_button")
                    ) {
                        Icon(imageVector = if (copiedCode) Icons.Default.Check else Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (copiedCode) "Copied!" else "Copy Sketch", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sampleBleArduinoCodeSnippet,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        if (showBleHc05ScannerDialog) {
            com.example.ui.components.BleHc05ScannerDialog(
                bleViewModel = bleScanningViewModel,
                onDismiss = { showBleHc05ScannerDialog = false }
            )
        }
    }
}

@Composable
private fun BleDeviceItemCard(
    device: BleDevice,
    isConnected: Boolean,
    onConnect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onConnect() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = device.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${device.deviceType} | ${device.address}", fontSize = 10.sp, color = Color.Gray)
                }

                if (isConnected) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EcoBadgeGood.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Connected",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoBadgeGood,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onConnect,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Connect GATT", fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Signal bar gauge & proximity label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        tint = when {
                            device.rssi >= -55 -> EcoBadgeGood
                            device.rssi >= -75 -> EcoBadgeWarning
                            else -> Color.Gray
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${device.rssi} dBm (${device.proximity})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (device.sensorData != null) {
                    Text(
                        text = "${device.sensorData.temperatureC}°C / ${device.sensorData.humidityPercent}% RH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = BioGreenLight
                    )
                }
            }
        }
    }
}

@Composable
private fun PairedDeviceRow(
    device: BluetoothDevice,
    onConnect: () -> Unit
) {
    val devName = try {
        device.name ?: "Unknown Bluetooth Device"
    } catch (_: SecurityException) {
        "Unknown Bluetooth Device"
    }
    val devAddress = device.address
    val isHc05 = devName.contains("HC-05", ignoreCase = true) ||
            devName.contains("HC05", ignoreCase = true) ||
            devName.contains("HC-06", ignoreCase = true) ||
            devName.contains("BT05", ignoreCase = true) ||
            devName.contains("Arduino", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isHc05) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isHc05) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onConnect() }
            .testTag("device_row_${devAddress.replace(":", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isHc05) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHc05) Icons.Default.DeveloperBoard else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (isHc05) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = devName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isHc05) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EcoBadgeGood.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "TARGET HC-05",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EcoBadgeGood,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = devAddress,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onConnect,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHc05) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isHc05) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("btn_connect_${devAddress.replace(":", "_")}")
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Connect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val sampleBleArduinoCodeSnippet = """
#include <ArduinoBLE.h>
BLEService ecoService("181A"); // Environmental Sensing
BLEFloatCharacteristic tempChar("2A6E", BLERead | BLENotify);
BLEFloatCharacteristic humChar("2A6F", BLERead | BLENotify);

void setup() {
  BLE.begin();
  BLE.setLocalName("Arduino Nano 33 BLE - EcoSense");
  BLE.setAdvertisedService(ecoService);
  BLE.advertise();
}
""".trimIndent()

private val sampleBleArduinoCode = """
#include <ArduinoBLE.h>
#include <DHT.h>

#define DHTPIN 7
#define DHTTYPE DHT22

DHT dht(DHTPIN, DHTTYPE);

BLEService ecoService("181A"); // Environmental Sensing Service
BLEFloatCharacteristic tempChar("2A6E", BLERead | BLENotify);
BLEFloatCharacteristic humChar("2A6F", BLERead | BLENotify);
BLEStringCharacteristic rfidChar("FFE1", BLERead | BLENotify, 20);

void setup() {
  Serial.begin(9600);
  dht.begin();

  if (!BLE.begin()) {
    Serial.println("Failed to start BLE!");
    while (1);
  }

  BLE.setLocalName("Arduino Nano 33 BLE - EcoSense");
  BLE.setAdvertisedService(ecoService);

  ecoService.addCharacteristic(tempChar);
  ecoService.addCharacteristic(humChar);
  ecoService.addCharacteristic(rfidChar);

  BLE.addService(ecoService);
  BLE.advertise();

  Serial.println("BLE Environmental Sensor Ready & Advertising...");
}

void loop() {
  BLEDevice central = BLE.central();
  if (central) {
    while (central.connected()) {
      float t = dht.readTemperature();
      float h = dht.readHumidity();

      if (!isnan(t)) tempChar.writeValue(t);
      if (!isnan(h)) humChar.writeValue(h);

      delay(2000);
    }
  }
}
""".trimIndent()

private val sampleNodeExpressServerCodeSnippet = """
const express = require('express');
const cors = require('cors');
const app = express();

app.use(cors({ origin: '*' }));
app.use(express.json());

app.get('/health', (req, res) => res.json({ status: "OK", server: "EcoMind Multi-Mobile Backend" }));
app.post('/sync-database', (req, res) => res.json({ message: "Multi-mobile DB synced successfully" }));

app.listen(3000, '0.0.0.0', () => console.log('Listening on http://0.0.0.0:3000'));
""".trimIndent()

private val sampleNodeExpressServerCode = """
const express = require('express');
const cors = require('cors');
const app = express();
const PORT = 3000;

// Enable CORS for ALL Mobile Devices on Local Network
app.use(cors({ origin: '*' }));
app.use(express.json());

let productsDb = [
  { id: "1001", name: "Recycled PET Bottle", category: "Plastic", carbon: "0.2 kg", water: "1.5 L", ecoScore: 88, recycling: "100%", impact: "Low Footprint", alternative: "Glass Bottle", isEcoFriendly: true },
  { id: "1002", name: "Organic Cotton Tote", category: "Clothing", carbon: "0.4 kg", water: "12 L", ecoScore: 92, recycling: "Compostable", impact: "Zero Plastic", alternative: "Paper Bag", isEcoFriendly: true }
];

let sensorReadingsDb = [];

// Health Check Endpoint
app.get('/health', (req, res) => res.json({ status: "OK", server: "EcoMind Multi-Mobile REST Backend", activeProducts: productsDb.length }));

// Get All Products
app.get('/products', (req, res) => res.json(productsDb));

// Get Single Product by RFID Tag ID
app.get('/product/:id', (req, res) => {
  const prod = productsDb.find(p => p.id === req.params.id);
  if (prod) res.json(prod);
  else res.status(404).json({ error: "Product not found" });
});

// Create / Update Product
app.post('/product', (req, res) => {
  const newProd = req.body;
  const existingIdx = productsDb.findIndex(p => p.id === newProd.id);
  if (existingIdx >= 0) productsDb[existingIdx] = newProd;
  else productsDb.push(newProd);
  res.json(newProd);
});

// Sync Full Mobile Room DB
app.post('/sync-database', (req, res) => {
  const { products, sensorReadings } = req.body;
  if (products && Array.isArray(products)) {
    products.forEach(p => {
      const idx = productsDb.findIndex(item => item.id === p.id);
      if (idx >= 0) productsDb[idx] = p;
      else productsDb.push(p);
    });
  }
  if (sensorReadings && Array.isArray(sensorReadings)) {
    sensorReadingsDb.push(...sensorReadings);
  }
  res.json({ message: "Multi-mobile database synced successfully", totalProducts: productsDb.length });
});

// Bind to 0.0.0.0 so ALL Mobile Devices on Wi-Fi/LAN can connect
app.listen(PORT, '0.0.0.0', () => {
  console.log(`EcoMind REST Server running on http://0.0.0.0:${'$'}PORT`);
});
""".trimIndent()

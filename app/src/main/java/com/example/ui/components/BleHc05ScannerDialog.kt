package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.BleDeviceCategory
import com.example.ui.BleDeviceConnectionStatus
import com.example.ui.BleScanStatus
import com.example.ui.BleScanningViewModel
import com.example.ui.DiscoveredBleDevice

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BleHc05ScannerDialog(
    bleViewModel: BleScanningViewModel,
    onDismiss: () -> Unit
) {
    val scanStatus by bleViewModel.scanStatus.collectAsState()
    val discoveredDevices by bleViewModel.filteredDevices.collectAsState()
    val isHc05Discovered by bleViewModel.isHc05Discovered.collectAsState()
    val filterHc05Only by bleViewModel.filterHc05Only.collectAsState()
    val connectionStatus by bleViewModel.connectionStatus.collectAsState()
    val connectedDevice by bleViewModel.connectedDevice.collectAsState()
    val latestTelemetry by bleViewModel.latestTelemetry.collectAsState()
    val autoSaveToDb by bleViewModel.autoSaveToDatabase.collectAsState()
    val rawLogStream by bleViewModel.rawLogStream.collectAsState()
    val packetsCount by bleViewModel.packetsReceivedCount.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Devices Scanner, 1: Live IoT Telemetry, 2: Serial Terminal
    var customCommandText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("dialog_ble_hc05_scanner"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00695C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BLE & HC-05 IoT Scanner",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Real-time environmental IoT data collection",
                                fontSize = 11.sp,
                                color = Color(0xFF00695C)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_ble_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Connection Status Strip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when (connectionStatus) {
                        is BleDeviceConnectionStatus.Connected -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                        is BleDeviceConnectionStatus.Connecting -> Color(0xFFFFA000).copy(alpha = 0.12f)
                        is BleDeviceConnectionStatus.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (connectionStatus) {
                                    is BleDeviceConnectionStatus.Connected -> Icons.Default.BluetoothConnected
                                    is BleDeviceConnectionStatus.Connecting -> Icons.Default.BluetoothSearching
                                    is BleDeviceConnectionStatus.Error -> Icons.Default.Warning
                                    else -> Icons.Default.Bluetooth
                                },
                                contentDescription = null,
                                tint = when (connectionStatus) {
                                    is BleDeviceConnectionStatus.Connected -> Color(0xFF2E7D32)
                                    is BleDeviceConnectionStatus.Connecting -> Color(0xFFFFA000)
                                    is BleDeviceConnectionStatus.Error -> MaterialTheme.colorScheme.error
                                    else -> Color.Gray
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when (val s = connectionStatus) {
                                        is BleDeviceConnectionStatus.Connected -> "Connected: ${s.deviceName}"
                                        is BleDeviceConnectionStatus.Connecting -> "Connecting to ${s.deviceName}..."
                                        is BleDeviceConnectionStatus.Error -> "Connection Error: ${s.message}"
                                        is BleDeviceConnectionStatus.Reconnecting -> "Reconnecting (${s.attempt}/${s.maxAttempts})..."
                                        else -> "Disconnected"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (connectionStatus is BleDeviceConnectionStatus.Connected) {
                                    Text(
                                        text = "${(connectionStatus as BleDeviceConnectionStatus.Connected).connectionMode} • $packetsCount packets RX",
                                        fontSize = 10.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        if (connectionStatus is BleDeviceConnectionStatus.Connected || connectionStatus is BleDeviceConnectionStatus.Connecting) {
                            OutlinedButton(
                                onClick = { bleViewModel.disconnect() },
                                modifier = Modifier.height(32.dp).testTag("btn_disconnect_ble")
                            ) {
                                Text("Disconnect", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { bleViewModel.quickConnectHc05() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                                modifier = Modifier.height(32.dp).testTag("btn_quick_connect_hc05")
                            ) {
                                Text("Auto HC-05", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Scanner (${discoveredDevices.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Live IoT Data", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Serial Console", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TAB 0: BLE Scanner
                if (selectedTab == 0) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Scan Controls Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (scanStatus is BleScanStatus.Scanning) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF00695C))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scanning...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                                } else {
                                    Text("BLE Device Discovery", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (scanStatus is BleScanStatus.Scanning) {
                                    Button(
                                        onClick = { bleViewModel.stopBleScan() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.height(34.dp).testTag("btn_stop_ble_scan")
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Stop", fontSize = 11.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { bleViewModel.startBleScan() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                                        modifier = Modifier.height(34.dp).testTag("btn_start_ble_scan")
                                    ) {
                                        Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Scan BLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = Color(0xFF00695C), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Filter HC-05 / IoT Nodes only", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Switch(
                                checked = filterHc05Only,
                                onCheckedChange = { bleViewModel.setFilterHc05Only(it) },
                                modifier = Modifier.testTag("switch_filter_hc05")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Discovered Devices List
                        if (discoveredDevices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (scanStatus is BleScanStatus.Scanning) "Listening for BLE advertisements..." else "No BLE devices found. Tap 'Scan BLE' to discover HC-05 modules.",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { bleViewModel.simulateTelemetryReading() },
                                        modifier = Modifier.testTag("btn_simulate_sample_packet")
                                    ) {
                                        Text("Simulate Sample IoT Packet", fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(discoveredDevices, key = { it.address }) { dev ->
                                    DiscoveredBleDeviceCard(
                                        device = dev,
                                        isCurrentConnected = connectedDevice?.address == dev.address && connectionStatus is BleDeviceConnectionStatus.Connected,
                                        onConnectClick = { bleViewModel.connectToDevice(dev) }
                                    )
                                }
                            }
                        }
                    }
                }

                // TAB 1: Live IoT Telemetry & Metrics
                if (selectedTab == 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Telemetry Summary Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Real-Time Telemetry Stream", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Auto-Save to DB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Switch(
                                    checked = autoSaveToDb,
                                    onCheckedChange = { bleViewModel.setAutoSaveToDatabase(it) },
                                    modifier = Modifier.testTag("switch_auto_save_db")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Metrics 2x2 Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TelemetryMetricCard(
                                label = "Temperature",
                                value = if (latestTelemetry.temperatureC > 0f) "%.1f °C".format(latestTelemetry.temperatureC) else "--",
                                icon = Icons.Default.Thermostat,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.weight(1f)
                            )
                            TelemetryMetricCard(
                                label = "Humidity",
                                value = if (latestTelemetry.humidityPercent > 0f) "%.1f %%".format(latestTelemetry.humidityPercent) else "--",
                                icon = Icons.Default.WaterDrop,
                                color = Color(0xFF0288D1),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TelemetryMetricCard(
                                label = "Air / Gas PPM",
                                value = if (latestTelemetry.gasPpm > 0f) "%.0f PPM".format(latestTelemetry.gasPpm) else "--",
                                icon = Icons.Default.Air,
                                color = Color(0xFF7B1FA2),
                                modifier = Modifier.weight(1f)
                            )
                            TelemetryMetricCard(
                                label = "Soil Moisture",
                                value = if (latestTelemetry.soilMoisturePercent > 0f) "%.1f %%".format(latestTelemetry.soilMoisturePercent) else "--",
                                icon = Icons.Default.Sensors,
                                color = Color(0xFF388E3C),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // RFID Scan Field
                        if (!latestTelemetry.scannedRfidTag.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("RFID / NFC Tag Detected", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        Text(latestTelemetry.scannedRfidTag!!, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Last Raw Payload Preview
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Latest Raw Stream Packet:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (latestTelemetry.rawPayload.isNotBlank()) latestTelemetry.rawPayload else "Awaiting stream from HC-05...",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Simulate Stream Action
                        OutlinedButton(
                            onClick = { bleViewModel.simulateTelemetryReading() },
                            modifier = Modifier.fillMaxWidth().testTag("btn_simulate_telemetry")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate Live IoT Reading (Test Node)", fontSize = 12.sp)
                        }
                    }
                }

                // TAB 2: Serial Console & Commands
                if (selectedTab == 2) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Quick Presets
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PresetCommandChip(label = "READ", onClick = { bleViewModel.sendIotCommand("READ") })
                            PresetCommandChip(label = "LED_ON", onClick = { bleViewModel.sendIotCommand("LED_ON") })
                            PresetCommandChip(label = "LED_OFF", onClick = { bleViewModel.sendIotCommand("LED_OFF") })
                            PresetCommandChip(label = "TARE", onClick = { bleViewModel.sendIotCommand("TARE") })
                            PresetCommandChip(label = "SCAN", onClick = { bleViewModel.sendIotCommand("SCAN") })
                            PresetCommandChip(label = "STATUS", onClick = { bleViewModel.sendIotCommand("STATUS") })
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Command Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customCommandText,
                                onValueChange = { customCommandText = it },
                                placeholder = { Text("Command (e.g. GET_TEMP)", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_ble_command")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if (customCommandText.isNotBlank()) {
                                        bleViewModel.sendIotCommand(customCommandText.trim())
                                        customCommandText = ""
                                    }
                                },
                                modifier = Modifier.testTag("btn_send_ble_command")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Serial Rx/Tx Stream Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { bleViewModel.clearLogs() }) {
                                Text("Clear", fontSize = 11.sp)
                            }
                        }

                        // Terminal Log Viewer
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E1E1E),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (rawLogStream.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No serial communication yet.", color = Color.Gray, fontSize = 11.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(rawLogStream) { line ->
                                        Text(
                                            text = line,
                                            color = if (line.contains("RX:")) Color(0xFF81C784) else if (line.contains("TX:")) Color(0xFF64B5F6) else Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredBleDeviceCard(
    device: DiscoveredBleDevice,
    isCurrentConnected: Boolean,
    onConnectClick: () -> Unit
) {
    val isHc05 = device.isHc05
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentConnected) {
            Color(0xFF2E7D32).copy(alpha = 0.12f)
        } else if (isHc05) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrentConnected) Color(0xFF2E7D32)
            else if (isHc05) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth().testTag("device_card_${device.address}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isHc05) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHc05) Icons.Default.DeveloperBoard else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (isHc05) Color.White else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isHc05) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "HC-05 IOT",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = device.address,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Signal: ${device.rssi} dBm • ${device.category.name.replace("_", " ")}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isCurrentConnected) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF2E7D32)
                ) {
                    Text(
                        text = "ACTIVE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHc05) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.height(34.dp).testTag("btn_connect_${device.address}")
                ) {
                    Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TelemetryMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PresetCommandChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

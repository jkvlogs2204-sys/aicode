package com.example.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.BluetoothState
import com.example.ui.EcoMindViewModel
import com.example.ui.theme.EcoBadgeBad
import com.example.ui.theme.EcoBadgeGood
import com.example.ui.theme.EcoBadgeWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothConnectionScreen(
    viewModel: EcoMindViewModel,
    onNavigateToDashboard: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bluetoothState by viewModel.bluetoothState.collectAsState()
    val isDiscovering by viewModel.isClassicDiscovering.collectAsState()
    val discoveredDevices by viewModel.discoveredClassicDevices.collectAsState()
    val pairedDevices = remember(bluetoothState) { viewModel.bluetoothManager.getPairedDevices() }
    val lastDevice = remember(bluetoothState) { viewModel.bluetoothManager.getLastConnectedDevice() }
    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val lastRawData by viewModel.lastRawData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Connect EcoMind Scanner",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HEADER SECTION
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Connect EcoMind Scanner",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Connect your EcoMind RFID scanner to begin scanning products.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            // 2. REAL-TIME CONNECTION STATUS CARD
            StatusCard(
                bluetoothState = bluetoothState,
                isDiscovering = isDiscovering,
                savedDeviceName = lastDevice?.second,
                onReconnect = {
                    if (lastDevice != null) {
                        viewModel.bluetoothManager.connectByAddress(lastDevice.first)
                    } else {
                        viewModel.startClassicDiscovery()
                    }
                },
                onStartScanning = onNavigateToDashboard
            )

            // 2.5 LIVE SERIAL TELEMETRY & SCANNED RFID CARD
            if (lastRawData.isNotBlank() || scannedProduct != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_live_rfid_telemetry")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LIVE RFID STREAM (HC-05)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            if (lastRawData.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "RAW: $lastRawData",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        scannedProduct?.let { product ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = EcoBadgeGood.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "TAG #${product.id}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = EcoBadgeGood,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = product.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${product.category} • ${product.carbon} • Eco Score: ${product.ecoScore}/100",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = onNavigateToDashboard,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.testTag("btn_view_scanned_details")
                                    ) {
                                        Text("VIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SCAN FOR HC-05 ACTION BUTTON
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HC-05 Scanner Discovery",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDiscovering) "Searching for nearby HC-05 / EcoMind modules..." else "Tap to discover nearby Bluetooth RFID readers",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (isDiscovering) {
                                viewModel.stopClassicDiscovery()
                            } else {
                                viewModel.startClassicDiscovery()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDiscovering) EcoBadgeWarning else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("btn_scan_hc05")
                    ) {
                        if (isDiscovering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STOP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SCAN FOR HC-05", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3.5 DIRECT MAC ADDRESS CONNECT
            var manualMacText by remember { mutableStateOf("") }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DIRECT CONNECT BY MAC ADDRESS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If HC-05 does not show in scan results, enter its Bluetooth MAC address directly (e.g. 00:21:13:00:27:14 or 98:D3:31:F8:3A:90).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = manualMacText,
                            onValueChange = { manualMacText = it },
                            placeholder = { Text("00:21:13:00:27:14", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualMacText.isNotBlank()) {
                                    viewModel.bluetoothManager.connectByAddress(manualMacText.trim())
                                }
                            },
                            enabled = manualMacText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_connect_manual_mac")
                        ) {
                            Text("CONNECT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. PRIORITIZED DISCOVERED & PAIRED DEVICES LIST
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE SCANNERS & DEVICES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${discoveredDevices.size + pairedDevices.size} Found",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val allDisplayDevices = remember(discoveredDevices, pairedDevices) {
                    val combined = (pairedDevices + discoveredDevices).distinctBy { it.address }
                    combined.sortedByDescending { dev ->
                        val name = (dev.name ?: "").uppercase()
                        when {
                            name.contains("HC-05") || name.contains("HC05") || name.contains("ECOMIND") -> 3
                            name.contains("HC-06") || name.contains("HC06") || name.contains("BT05") -> 2
                            name.contains("ARDUINO") -> 1
                            else -> 0
                        }
                    }
                }

                if (allDisplayDevices.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Bluetooth Scanners Discovered Yet",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ensure HC-05 is powered ON (LED blinking fast) and tap 'SCAN FOR HC-05'.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    allDisplayDevices.forEach { device ->
                        DeviceListItem(
                            device = device,
                            bluetoothState = bluetoothState,
                            onConnect = { viewModel.bluetoothManager.connectToDevice(device) },
                            onDisconnect = { viewModel.bluetoothManager.disconnect() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // 5. FOOTER ARCHITECTURE NOTE
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Unidirectional Hardware Pipeline",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "RFID Tag → MFRC522 → Arduino → HC-05 → EcoMind App. Zero reverse signals sent to Arduino.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    bluetoothState: BluetoothState,
    isDiscovering: Boolean,
    savedDeviceName: String?,
    onReconnect: () -> Unit,
    onStartScanning: () -> Unit
) {
    val (statusColor, statusTitle, statusSubtitle, statusDot) = when (bluetoothState) {
        is BluetoothState.Connected -> Quadruple(
            EcoBadgeGood,
            "🟢 HC-05 Connected",
            "RFID Scanner Ready • Connected to ${bluetoothState.deviceName}",
            EcoBadgeGood
        )
        is BluetoothState.Connecting -> Quadruple(
            EcoBadgeWarning,
            "🟡 Connecting to HC-05...",
            "Establishing serial RFCOMM socket link to ${bluetoothState.deviceName}...",
            EcoBadgeWarning
        )
        is BluetoothState.Reconnecting -> Quadruple(
            EcoBadgeWarning,
            "🟡 HC-05 Reconnecting...",
            "Attempt ${bluetoothState.attempt}/${bluetoothState.maxAttempts} to reconnect to ${bluetoothState.deviceName}...",
            EcoBadgeWarning
        )
        is BluetoothState.Error -> Quadruple(
            EcoBadgeBad,
            "🔴 Connection Failed",
            bluetoothState.message,
            EcoBadgeBad
        )
        is BluetoothState.Disconnected -> if (isDiscovering) {
            Quadruple(
                EcoBadgeWarning,
                "🟡 Searching for HC-05",
                "Scanning nearby Classic Bluetooth frequency band for RFID scanners...",
                EcoBadgeWarning
            )
        } else {
            Quadruple(
                Color.Gray,
                "⚪ Disconnected",
                if (savedDeviceName != null) "Last device: $savedDeviceName. Tap RECONNECT to pair." else "Bluetooth scanner disconnected.",
                Color.Gray
            )
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusDot)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when (bluetoothState) {
                            is BluetoothState.Connected -> "READY"
                            is BluetoothState.Connecting, is BluetoothState.Reconnecting -> "CONNECTING"
                            is BluetoothState.Error -> "ERROR"
                            else -> if (isDiscovering) "SEARCHING" else "DISCONNECTED"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusSubtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            // ACTION BUTTONS BASED ON STATE
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (bluetoothState) {
                    is BluetoothState.Connected -> {
                        Button(
                            onClick = onStartScanning,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EcoBadgeGood),
                            modifier = Modifier.testTag("btn_start_scanning")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("START SCANNING", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    is BluetoothState.Error, is BluetoothState.Disconnected -> {
                        if (savedDeviceName != null) {
                            OutlinedButton(
                                onClick = onReconnect,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_reconnect")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RECONNECT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: BluetoothDevice,
    bluetoothState: BluetoothState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val rawName = try { device.name ?: "" } catch (e: Exception) { "" }
    val devName = if (rawName.isBlank()) "Unknown Bluetooth Device" else rawName
    val isHc05 = devName.uppercase().contains("HC-05") || devName.uppercase().contains("HC05") || devName.uppercase().contains("ECOMIND")
    val isConnected = bluetoothState is BluetoothState.Connected && bluetoothState.deviceAddress == device.address
    val isConnecting = (bluetoothState is BluetoothState.Connecting && bluetoothState.deviceName.contains(devName)) ||
            (bluetoothState is BluetoothState.Reconnecting && bluetoothState.deviceName.contains(devName))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) EcoBadgeGood.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isConnected || isHc05) 1.5.dp else 1.dp,
            color = when {
                isConnected -> EcoBadgeGood
                isHc05 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier.fillMaxWidth()
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
                        .background(
                            when {
                                isConnected -> EcoBadgeGood.copy(alpha = 0.15f)
                                isHc05 -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = when {
                            isConnected -> EcoBadgeGood
                            isHc05 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = devName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isHc05) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "HC-05",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = device.address,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            when {
                isConnected -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EcoBadgeGood.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { onDisconnect() }
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CONNECTED ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoBadgeGood
                            )
                        }
                    }
                }
                isConnecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    Button(
                        onClick = onConnect,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHc05) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            text = "CONNECT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

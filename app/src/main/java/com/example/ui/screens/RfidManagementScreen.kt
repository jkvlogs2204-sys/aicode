package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.EcoBadgeGood
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RfidMappingEntity
import com.example.ui.EcoMindViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RfidManagementScreen(
    viewModel: EcoMindViewModel,
    onNavigateToDashboard: () -> Unit = {}
) {
    val mappings by viewModel.rfidMappings.collectAsState()
    val scannedProduct by viewModel.scannedProduct.collectAsState()

    val isNfcServiceRunning by viewModel.isNfcServiceRunning.collectAsState()
    val nfcServiceStatus by viewModel.nfcServiceStatus.collectAsState()
    val nfcScansHistory by viewModel.nfcScansHistory.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedZoneFilter by remember { mutableStateOf("All") }
    var showEditDialog by remember { mutableStateOf(false) }
    var mappingToEdit by remember { mutableStateOf<RfidMappingEntity?>(null) }

    val zonesList = remember(mappings) {
        listOf("All") + mappings.map { it.zoneName }.distinct().sorted()
    }

    val filteredMappings = remember(mappings, searchQuery, selectedZoneFilter) {
        mappings.filter { m ->
            val matchesZone = (selectedZoneFilter == "All" || m.zoneName == selectedZoneFilter)
            val matchesSearch = searchQuery.isBlank() ||
                    m.tagId.contains(searchQuery, ignoreCase = true) ||
                    m.zoneName.contains(searchQuery, ignoreCase = true) ||
                    m.assignedDevice.contains(searchQuery, ignoreCase = true) ||
                    m.notes.contains(searchQuery, ignoreCase = true)
            matchesZone && matchesSearch
        }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("rfid_management_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp)
        ) {
            // Header Stats Banner
            item {
                RfidHeaderStatsCard(
                    totalMapped = mappings.size,
                    totalZones = mappings.map { it.zoneName }.distinct().size,
                    totalDevices = mappings.map { it.assignedDevice }.distinct().size,
                    onAddNew = {
                        mappingToEdit = null
                        showEditDialog = true
                    },
                    onSeedDefault = { viewModel.seedDefaultRfidMappings() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // NFC Background Listener & Firestore Sync Card
            item {
                NfcBackgroundScannerCard(
                    isServiceRunning = isNfcServiceRunning,
                    status = nfcServiceStatus,
                    scanHistory = nfcScansHistory,
                    onStartService = { viewModel.startNfcBackgroundService() },
                    onStopService = { viewModel.stopNfcBackgroundService() },
                    onTriggerScan = { viewModel.triggerPhysicalNfcScan() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Search Bar & Zone Filter Row
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Tag ID, Zone, or Arduino Device...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_rfid_mappings")
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (zonesList.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(zonesList) { zone ->
                            FilterChip(
                                selected = selectedZoneFilter == zone,
                                onClick = { selectedZoneFilter = zone },
                                label = { Text(zone, fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_zone_$zone")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // List Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mapped RFID Devices (${filteredMappings.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap 'Scan' to trigger active tag",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Mapping Items
            if (filteredMappings.isEmpty()) {
                item {
                    EmptyRfidStateCard(
                        onAddFirst = {
                            mappingToEdit = null
                            showEditDialog = true
                        }
                    )
                }
            } else {
                items(filteredMappings, key = { it.tagId }) { mapping ->
                    RfidMappingItemCard(
                        mapping = mapping,
                        isCurrentlyScanned = scannedProduct?.id == mapping.tagId,
                        onSelectTag = {
                            viewModel.manualRfidScan(mapping.tagId)
                            onNavigateToDashboard()
                        },
                        onEdit = {
                            mappingToEdit = mapping
                            showEditDialog = true
                        },
                        onDelete = { viewModel.deleteRfidMapping(mapping.tagId) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Floating Action Button to Register RFID Tag
        ExtendedFloatingActionButton(
            onClick = {
                mappingToEdit = null
                showEditDialog = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = "Register RFID Tag") },
            text = { Text("Register Tag", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_add_rfid")
        )

        // Add/Edit Dialog
        if (showEditDialog) {
            AddEditRfidMappingDialog(
                initialMapping = mappingToEdit,
                lastScannedTagId = scannedProduct?.id,
                onDismiss = { showEditDialog = false },
                onSave = { tagId, zone, device, notes ->
                    viewModel.saveRfidMapping(tagId, zone, device, notes)
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
private fun RfidHeaderStatsCard(
    totalMapped: Int,
    totalZones: Int,
    totalDevices: Int,
    onAddNew: () -> Unit,
    onSeedDefault: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().testTag("rfid_stats_header_card")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EcoBadgeGood.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EcoBadgeGood.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = EcoBadgeGood, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RFID & HARDWARE MAPPING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EcoBadgeGood)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "RFID Zone & Device Mapper",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "Map physical RFID hardware tags to IoT zones and Arduino sensor nodes",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stat Counter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill(count = "$totalMapped", label = "Mapped Tags", icon = Icons.Default.QrCode)
                    StatPill(count = "$totalZones", label = "IoT Zones", icon = Icons.Default.LocationOn)
                    StatPill(count = "$totalDevices", label = "Arduino Nodes", icon = Icons.Default.Memory)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onSeedDefault,
                        modifier = Modifier.testTag("btn_seed_default_rfid")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = EcoBadgeGood)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Seed Defaults", fontSize = 11.sp, color = EcoBadgeGood, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAddNew,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = EcoBadgeGood),
                        modifier = Modifier.testTag("btn_add_rfid_mapping")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Map New Tag", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(count: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = count, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = label, fontSize = 9.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun RfidMappingItemCard(
    mapping: RfidMappingEntity,
    isCurrentlyScanned: Boolean,
    onSelectTag: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyScanned)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_rfid_mapping_${mapping.tagId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tag ID Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "TAG: ${mapping.tagId}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (isCurrentlyScanned) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2E7D32)
                        ) {
                            Text(
                                text = "ACTIVE SCAN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Edit / Delete Actions
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp).testTag("btn_edit_rfid_${mapping.tagId}")) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp).testTag("btn_delete_rfid_${mapping.tagId}")) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mapped Zone & Mapped Hardware Device Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Environmental Zone Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = "Zone", fontSize = 8.sp, color = Color.Gray)
                            Text(
                                text = mapping.zoneName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Arduino Hardware Device Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = "Hardware Device", fontSize = 8.sp, color = Color.Gray)
                            Text(
                                text = mapping.assignedDevice,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            if (mapping.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${mapping.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedDate = remember(mapping.lastUpdated) {
                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(mapping.lastUpdated))
                }
                Text(
                    text = "Updated: $formattedDate",
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                OutlinedButton(
                    onClick = onSelectTag,
                    modifier = Modifier.testTag("btn_scan_mapping_${mapping.tagId}")
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan Tag", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyRfidStateCard(onAddFirst: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No RFID Zone Mappings Found",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Map your physical RFID tag IDs to environmental zones or Arduino devices for instant visual identification on the Dashboard.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onAddFirst, modifier = Modifier.testTag("btn_add_first_rfid")) {
                Text("Add First Mapping")
            }
        }
    }
}

@Composable
private fun AddEditRfidMappingDialog(
    initialMapping: RfidMappingEntity?,
    lastScannedTagId: String?,
    onDismiss: () -> Unit,
    onSave: (tagId: String, zone: String, device: String, notes: String) -> Unit
) {
    var tagId by remember { mutableStateOf(initialMapping?.tagId ?: "") }
    var zoneName by remember { mutableStateOf(initialMapping?.zoneName ?: "Zone A - Hydroponics Facility") }
    var assignedDevice by remember { mutableStateOf(initialMapping?.assignedDevice ?: "Arduino Node #1 (DHT22 & RC522)") }
    var notes by remember { mutableStateOf(initialMapping?.notes ?: "") }

    val zonePresets = listOf(
        "Zone A - Hydroponics Facility",
        "Zone B - Server Room",
        "Zone C - Zero-Waste Hub",
        "Zone D - E-Waste Sorting Room",
        "Zone E - Compost Facility"
    )

    val devicePresets = listOf(
        "Arduino Node #1 (DHT22 & RC522)",
        "HC-05 Serial Module #2",
        "ESP32 BLE Sensor Node",
        "Arduino Mega RFID Scanner"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialMapping != null) "Edit RFID Tag Mapping" else "Map RFID Tag to Zone",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Tag ID Input
                OutlinedTextField(
                    value = tagId,
                    onValueChange = { tagId = it },
                    label = { Text("RFID Tag ID (e.g. 1001)") },
                    singleLine = true,
                    enabled = initialMapping == null, // Lock ID on edit
                    modifier = Modifier.fillMaxWidth().testTag("input_rfid_tag_id")
                )

                if (initialMapping == null && !lastScannedTagId.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { tagId = lastScannedTagId },
                        modifier = Modifier.testTag("btn_use_last_scanned_tag")
                    ) {
                        Text("Use last scanned Tag ID ($lastScannedTagId)", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Zone Name Input & Presets
                Text("Environmental Zone:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    label = { Text("Zone Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_rfid_zone_name")
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(zonePresets) { preset ->
                        FilterChip(
                            selected = zoneName == preset,
                            onClick = { zoneName = preset },
                            label = { Text(preset.split(" - ").last(), fontSize = 10.sp) },
                            modifier = Modifier.testTag("chip_preset_zone_${preset.take(6)}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Arduino Hardware Device & Presets
                Text("Arduino Hardware Node:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = assignedDevice,
                    onValueChange = { assignedDevice = it },
                    label = { Text("Arduino Device") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_rfid_assigned_device")
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(devicePresets) { devPreset ->
                        FilterChip(
                            selected = assignedDevice == devPreset,
                            onClick = { assignedDevice = devPreset },
                            label = { Text(devPreset.split(" ").first(), fontSize = 10.sp) },
                            modifier = Modifier.testTag("chip_preset_dev_${devPreset.take(6)}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Deployment Details (Optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_rfid_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tagId.isNotBlank()) {
                        onSave(tagId, zoneName, assignedDevice, notes)
                    }
                },
                enabled = tagId.isNotBlank(),
                modifier = Modifier.testTag("btn_save_rfid_mapping")
            ) {
                Text("Save Mapping")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_cancel_rfid_mapping")) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NfcBackgroundScannerCard(
    isServiceRunning: Boolean,
    status: com.example.nfc.NfcServiceStatus,
    scanHistory: List<com.example.nfc.NfcRfidTagScan>,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTriggerScan: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_nfc_background_service")
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
                            .clip(CircleShape)
                            .background(
                                if (isServiceRunning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NFC Background Scanner Service",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isServiceRunning) "Foreground Service ACTIVE - Tag scans link to Firestore" else "Background NfcAdapter Listener Off",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = { checked ->
                        if (checked) onStartService() else onStopService()
                    },
                    modifier = Modifier.testTag("btn_toggle_nfc_service")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isServiceRunning) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NfcAdapter Mode: ACTIVE (ReaderMode)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val statusText = when (status) {
                                is com.example.nfc.NfcServiceStatus.TagDetected -> "Scanned: ${status.scan.tagId} -> ${status.scan.locationName}"
                                is com.example.nfc.NfcServiceStatus.ActiveScanning -> "Ready - Place physical RFID tag on device"
                                else -> "Service Running"
                            }
                            Text(
                                text = statusText,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onTriggerScan,
                            modifier = Modifier.testTag("btn_scan_nfc_tag")
                        ) {
                            Text("Scan Tag", fontSize = 11.sp)
                        }
                    }
                }

                if (scanHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Recent Scans & Firestore Location Links:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    scanHistory.take(3).forEach { scan ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${scan.tagId} -> ${scan.locationName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${scan.temperatureC}°C | ${scan.humidityPercent}% Hum | ${scan.co2Ppm.toInt()} PPM CO2",
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (scan.isFirestoreLinked) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (scan.isFirestoreLinked) "FIRESTORE LINKED" else "LOCAL",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (scan.isFirestoreLinked) Color(0xFF2E7D32) else Color.DarkGray,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

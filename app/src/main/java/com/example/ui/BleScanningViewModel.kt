package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EcoDatabase
import com.example.data.FirestoreSyncHelper
import com.example.data.SensorReadingEntity
import com.example.data.SensorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Categorization of Bluetooth Low Energy & IoT peripherals.
 */
enum class BleDeviceCategory {
    HC05_MODULE,
    HC06_MODULE,
    HC08_BLE,
    HM10_BLE,
    ARDUINO_BLE,
    ESP32_BLE,
    ENVIRONMENTAL_SENSOR,
    GENERIC_BLE
}

/**
 * Discovered BLE Device representation.
 */
data class DiscoveredBleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val category: BleDeviceCategory,
    val isHc05: Boolean,
    val serviceUuids: List<String> = emptyList(),
    val lastSeenMs: Long = System.currentTimeMillis()
)

/**
 * Status of the BLE Scanner.
 */
sealed class BleScanStatus {
    object Idle : BleScanStatus()
    data class Scanning(val foundCount: Int) : BleScanStatus()
    data class Stopped(val totalCount: Int) : BleScanStatus()
    data class Error(val message: String) : BleScanStatus()
}

/**
 * Connection status to the target HC-05 or BLE device.
 */
sealed class BleDeviceConnectionStatus {
    object Disconnected : BleDeviceConnectionStatus()
    data class Connecting(val deviceName: String, val address: String) : BleDeviceConnectionStatus()
    data class Connected(
        val deviceName: String,
        val address: String,
        val connectionMode: String // "BLE GATT (Low Energy)" or "Bluetooth Classic SPP (RFCOMM)"
    ) : BleDeviceConnectionStatus()
    data class Reconnecting(val deviceName: String, val attempt: Int, val maxAttempts: Int = 3) : BleDeviceConnectionStatus()
    data class Error(val message: String) : BleDeviceConnectionStatus()
}

/**
 * Real-time IoT Telemetry collected from the connected HC-05 / BLE node.
 */
data class RealTimeIotTelemetry(
    val temperatureC: Float = 0.0f,
    val humidityPercent: Float = 0.0f,
    val gasPpm: Float = 0.0f,
    val soilMoisturePercent: Float = 0.0f,
    val waterLevelPercent: Float = 0.0f,
    val lightLux: Float = 0.0f,
    val scannedRfidTag: String? = null,
    val rawPayload: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val packetCount: Long = 0
)

/**
 * Dedicated ViewModel for Bluetooth Low Energy (BLE) scanning,
 * discovery of HC-05/HC-06/HC-08 modules, resilient GATT/SPP connection,
 * and continuous real-time IoT environmental telemetry data collection.
 */
class BleScanningViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BleScanningViewModel"

    // SPP UUID standard for HC-05 / HC-06 Bluetooth Serial Port Profile
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    // Client Characteristic Configuration Descriptor
    private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    // Database & Repository for persisting incoming IoT data
    private val db = EcoDatabase.getDatabase(application)
    private val firestoreSyncHelper = FirestoreSyncHelper(db.sensorReadingDao(), db.productDao(), db.rfidMappingDao())
    private val sensorRepository = SensorRepository(db.sensorReadingDao(), firestoreSyncHelper)

    @Suppress("DEPRECATION")
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // Scanning States
    private val _scanStatus = MutableStateFlow<BleScanStatus>(BleScanStatus.Idle)
    val scanStatus: StateFlow<BleScanStatus> = _scanStatus.asStateFlow()

    private val _discoveredMap = mutableMapOf<String, DiscoveredBleDevice>()
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _filterHc05Only = MutableStateFlow(false)
    val filterHc05Only: StateFlow<Boolean> = _filterHc05Only.asStateFlow()

    // Filtered devices flow
    val filteredDevices: StateFlow<List<DiscoveredBleDevice>> = combine(
        _discoveredDevices,
        _filterHc05Only
    ) { devices, hc05Only ->
        if (hc05Only) devices.filter { it.isHc05 } else devices
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks if at least one HC-05 device is currently discovered
    val isHc05Discovered: StateFlow<Boolean> = _discoveredDevices.combine(_filterHc05Only) { devices, _ ->
        devices.any { it.isHc05 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Connection States
    private val _connectionStatus = MutableStateFlow<BleDeviceConnectionStatus>(BleDeviceConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<BleDeviceConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedDevice = MutableStateFlow<DiscoveredBleDevice?>(null)
    val connectedDevice: StateFlow<DiscoveredBleDevice?> = _connectedDevice.asStateFlow()

    // Real-Time Telemetry States
    private val _latestTelemetry = MutableStateFlow(RealTimeIotTelemetry())
    val latestTelemetry: StateFlow<RealTimeIotTelemetry> = _latestTelemetry.asStateFlow()

    private val _telemetryHistory = MutableStateFlow<List<RealTimeIotTelemetry>>(emptyList())
    val telemetryHistory: StateFlow<List<RealTimeIotTelemetry>> = _telemetryHistory.asStateFlow()

    private val _isCollectingData = MutableStateFlow(false)
    val isCollectingData: StateFlow<Boolean> = _isCollectingData.asStateFlow()

    private val _autoSaveToDatabase = MutableStateFlow(true)
    val autoSaveToDatabase: StateFlow<Boolean> = _autoSaveToDatabase.asStateFlow()

    private val _rawLogStream = MutableStateFlow<List<String>>(emptyList())
    val rawLogStream: StateFlow<List<String>> = _rawLogStream.asStateFlow()

    private val _scannedRfidFlow = MutableSharedFlow<String>(replay = 0)
    val scannedRfidFlow: SharedFlow<String> = _scannedRfidFlow.asSharedFlow()

    private val _packetsReceivedCount = MutableStateFlow(0L)
    val packetsReceivedCount: StateFlow<Long> = _packetsReceivedCount.asStateFlow()

    private val _commandsSentCount = MutableStateFlow(0)
    val commandsSentCount: StateFlow<Int> = _commandsSentCount.asStateFlow()

    // Hardware handles
    private var activeGatt: BluetoothGatt? = null
    private var activeSocket: BluetoothSocket? = null
    private var socketOutputStream: OutputStream? = null
    private var dataStreamJob: Job? = null
    private var isScanningHardware = false

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    /**
     * BLE Scan Callback
     */
    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { res ->
                val device = res.device ?: return
                val address = device.address ?: return
                val name = device.name ?: res.scanRecord?.deviceName ?: "BLE Peripheral ($address)"
                val rssi = res.rssi
                val serviceUuids = res.scanRecord?.serviceUuids?.map { it.toString() } ?: emptyList()

                val category = determineCategory(name, serviceUuids)
                val isHc05 = checkIfHc05(name, address)

                val item = DiscoveredBleDevice(
                    name = name,
                    address = address,
                    rssi = rssi,
                    category = category,
                    isHc05 = isHc05,
                    serviceUuids = serviceUuids,
                    lastSeenMs = System.currentTimeMillis()
                )

                _discoveredMap[address] = item
                updateDiscoveredList()
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { onScanResult(0, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed with error code $errorCode")
            _scanStatus.value = BleScanStatus.Error("BLE scan failed with code $errorCode")
        }
    }

    /**
     * Start Bluetooth Low Energy (BLE) scanning.
     * Also checks paired/bonded devices to immediately populate known HC-05 modules.
     */
    @SuppressLint("MissingPermission")
    fun startBleScan() {
        if (_scanStatus.value is BleScanStatus.Scanning) {
            stopBleScan()
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _scanStatus.value = BleScanStatus.Error("Bluetooth is turned off or not supported")
            return
        }

        _discoveredMap.clear()

        // Include paired Bluetooth devices (especially paired HC-05 modules)
        try {
            bluetoothAdapter.bondedDevices?.forEach { device ->
                val name = device.name ?: "Paired Device"
                val address = device.address
                val isHc05 = checkIfHc05(name, address)
                val category = if (isHc05) BleDeviceCategory.HC05_MODULE else BleDeviceCategory.GENERIC_BLE

                _discoveredMap[address] = DiscoveredBleDevice(
                    name = name,
                    address = address,
                    rssi = -55,
                    category = category,
                    isHc05 = isHc05,
                    lastSeenMs = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query bonded devices: ${e.message}")
        }

        updateDiscoveredList()
        _scanStatus.value = BleScanStatus.Scanning(_discoveredMap.size)

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner != null) {
            try {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                scanner.startScan(null, settings, bleScanCallback)
                isScanningHardware = true
                Log.d(TAG, "Hardware BLE scan started successfully")
                addLog("Started BLE scan for IoT & HC-05 devices...")
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting BLE scan: ${e.message}")
                _scanStatus.value = BleScanStatus.Error("Scan failed: ${e.localizedMessage}")
            }
        } else {
            _scanStatus.value = BleScanStatus.Error("BLE Scanner not available on this device")
        }
    }

    /**
     * Stop BLE scanning.
     */
    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        if (isScanningHardware) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleScanCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping BLE scan: ${e.message}")
            }
            isScanningHardware = false
        }
        val count = _discoveredMap.size
        _scanStatus.value = BleScanStatus.Stopped(count)
        addLog("BLE scan stopped. Found $count devices.")
    }

    /**
     * Connect to a discovered device.
     * Resilient hybrid approach:
     * 1) Tries BLE GATT connection.
     * 2) If device is an HC-05 Classic SPP or GATT fails, connects via RFCOMM SPP socket.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: DiscoveredBleDevice) {
        stopBleScan()
        disconnect()

        _connectedDevice.value = device
        _connectionStatus.value = BleDeviceConnectionStatus.Connecting(device.name, device.address)
        addLog("Connecting to ${device.name} (${device.address})...")

        viewModelScope.launch(Dispatchers.IO) {
            // If explicitly HC-05 / HC-06 Classic SPP, attempt SPP socket first
            if (device.category == BleDeviceCategory.HC05_MODULE || device.category == BleDeviceCategory.HC06_MODULE) {
                val sppSuccess = attemptSppConnection(device)
                if (!sppSuccess) {
                    Log.d(TAG, "SPP failed, attempting BLE GATT fallback...")
                    attemptGattConnection(device)
                }
            } else {
                // Try BLE GATT first
                attemptGattConnection(device)
            }
        }
    }

    /**
     * One-tap auto-connect: finds the first HC-05 device in range and connects.
     */
    fun quickConnectHc05() {
        val hc05 = _discoveredDevices.value.firstOrNull { it.isHc05 }
        if (hc05 != null) {
            connectToDevice(hc05)
        } else {
            addLog("Scanning to discover HC-05 module...")
            startBleScan()
            viewModelScope.launch {
                // Wait up to 5 seconds for HC-05 discovery
                for (i in 1..10) {
                    delay(500)
                    val found = _discoveredDevices.value.firstOrNull { it.isHc05 }
                    if (found != null) {
                        addLog("Discovered ${found.name}! Connecting now...")
                        connectToDevice(found)
                        return@launch
                    }
                }
                addLog("No HC-05 module discovered yet. Keep device close.")
            }
        }
    }

    /**
     * Connect via Classic Bluetooth SPP (RFCOMM) for standard HC-05 modules.
     */
    @SuppressLint("MissingPermission")
    private suspend fun attemptSppConnection(device: DiscoveredBleDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            val remoteDevice = bluetoothAdapter?.getRemoteDevice(device.address) ?: return@withContext false
            bluetoothAdapter.cancelDiscovery()

            val socket = remoteDevice.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            activeSocket = socket
            socketOutputStream = socket.outputStream

            _connectionStatus.value = BleDeviceConnectionStatus.Connected(
                deviceName = device.name,
                address = device.address,
                connectionMode = "Bluetooth Classic SPP (HC-05)"
            )
            _isCollectingData.value = true
            addLog("Connected to ${device.name} via RFCOMM SPP!")

            // Start reading incoming data stream
            startSppDataStreamReader(socket)
            true
        } catch (e: Exception) {
            Log.w(TAG, "SPP connection failed: ${e.message}")
            false
        }
    }

    /**
     * Connect via Bluetooth Low Energy (GATT).
     */
    @SuppressLint("MissingPermission")
    private fun attemptGattConnection(device: DiscoveredBleDevice) {
        try {
            val remoteDevice = bluetoothAdapter?.getRemoteDevice(device.address)
            if (remoteDevice == null) {
                _connectionStatus.value = BleDeviceConnectionStatus.Error("Device address not valid")
                return
            }

            activeGatt = remoteDevice.connectGatt(getApplication(), false, gattCallback)
        } catch (e: Exception) {
            Log.e(TAG, "GATT connection exception: ${e.message}")
            _connectionStatus.value = BleDeviceConnectionStatus.Error("GATT failed: ${e.localizedMessage}")
        }
    }

    /**
     * GATT Callback for BLE peripherals.
     */
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val dev = _connectedDevice.value
            val devName = dev?.name ?: gatt?.device?.name ?: "BLE Node"

            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "GATT Connected to $devName. Discovering services...")
                        addLog("GATT Connected to $devName. Discovering services...")
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(TAG, "GATT Disconnected from $devName")
                        addLog("Disconnected from $devName")
                        disconnect()
                    }
                }
            } else {
                Log.e(TAG, "GATT connection status error: $status")
                _connectionStatus.value = BleDeviceConnectionStatus.Error("Connection error (Status $status)")
                disconnect()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val dev = _connectedDevice.value ?: DiscoveredBleDevice(
                    name = gatt.device.name ?: "BLE Node",
                    address = gatt.device.address,
                    rssi = -60,
                    category = BleDeviceCategory.GENERIC_BLE,
                    isHc05 = false
                )

                _connectionStatus.value = BleDeviceConnectionStatus.Connected(
                    deviceName = dev.name,
                    address = dev.address,
                    connectionMode = "BLE GATT (Low Energy)"
                )
                _isCollectingData.value = true
                addLog("GATT Services discovered (${gatt.services.size} services). Ready for IoT telemetry.")

                // Enable notifications on UART and Environmental characteristics
                gatt.services.forEach { service ->
                    service.characteristics.forEach { characteristic ->
                        enableNotificationIfSupported(gatt, characteristic)
                    }
                }
            } else {
                _connectionStatus.value = BleDeviceConnectionStatus.Error("Service discovery failed (Status $status)")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.let { parseCharacteristicData(it) }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                parseCharacteristicData(characteristic)
            }
        }
    }

    /**
     * Enables notification or indication on a GATT characteristic.
     */
    @SuppressLint("MissingPermission")
    private fun enableNotificationIfSupported(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val props = characteristic.properties
        val isNotify = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
        val isIndicate = (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0

        if (isNotify || isIndicate) {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                @Suppress("DEPRECATION")
                descriptor.value = if (isNotify) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                }
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
                Log.d(TAG, "Notification registered on characteristic: ${characteristic.uuid}")
            }
        }
    }

    /**
     * Reads incoming lines from SPP Socket (HC-05 / HC-06).
     */
    private fun startSppDataStreamReader(socket: BluetoothSocket) {
        dataStreamJob?.cancel()
        dataStreamJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                while (isActive && socket.isConnected) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) {
                        processIncomingTelemetryString(line.trim())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "SPP Data stream closed: ${e.message}")
            } finally {
                if (_connectionStatus.value is BleDeviceConnectionStatus.Connected) {
                    _connectionStatus.value = BleDeviceConnectionStatus.Disconnected
                    _isCollectingData.value = false
                }
            }
        }
    }

    /**
     * Parses GATT characteristic byte payload.
     */
    private fun parseCharacteristicData(characteristic: BluetoothGattCharacteristic) {
        @Suppress("DEPRECATION")
        val bytes = characteristic.value ?: return
        val uuidStr = characteristic.uuid.toString().uppercase()

        when {
            uuidStr.contains("2A6E") -> { // Environmental Sensing: Temperature (0.01 °C)
                val temp = if (bytes.size >= 2) {
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short / 100f
                } else if (bytes.isNotEmpty()) bytes[0].toFloat() else 0f
                updateTelemetry { copy(temperatureC = temp) }
            }
            uuidStr.contains("2A6F") -> { // Environmental Sensing: Humidity (0.01 %)
                val hum = if (bytes.size >= 2) {
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short / 100f
                } else if (bytes.isNotEmpty()) bytes[0].toFloat() else 0f
                updateTelemetry { copy(humidityPercent = hum) }
            }
            else -> { // Raw text or UART (HM-10 FFE1, Nordic UART, ESP32)
                val text = String(bytes).trim()
                if (text.isNotEmpty()) {
                    processIncomingTelemetryString(text)
                }
            }
        }
    }

    /**
     * Core IoT Telemetry Decoder.
     * Decodes diverse incoming data formats:
     * 1) CSV key-value: "TEMP:24.5,HUM:55.2,GAS:120.4,SOIL:65,LIGHT:350"
     * 2) Pipe: "T:24.5|H:55.2|G:120.4"
     * 3) RFID tag scan: "PRODUCT:E28011606000020473919424" or "RFID:A1B2C3D4"
     * 4) JSON: {"temp":24.5,"hum":55.2,"gas":120.4}
     */
    fun processIncomingTelemetryString(payload: String) {
        addLog("RX: $payload")
        _packetsReceivedCount.value += 1

        var temp: Float? = null
        var hum: Float? = null
        var gas: Float? = null
        var soil: Float? = null
        var water: Float? = null
        var light: Float? = null
        var rfidTag: String? = null

        val upper = payload.uppercase()

        // 1. RFID Tag Detection
        if (upper.startsWith("PRODUCT:") || upper.startsWith("RFID:") || upper.startsWith("TAG:")) {
            val tag = payload.substringAfter(":").trim()
            if (tag.isNotEmpty()) {
                rfidTag = tag
                viewModelScope.launch {
                    _scannedRfidFlow.emit(tag)
                }
            }
        }

        // 2. Key-Value pairs separated by comma or semicolon or pipe
        val tokens = payload.split(Regex("[,;|]"))
        for (token in tokens) {
            val part = token.trim()
            if (part.contains(":")) {
                val key = part.substringBefore(":").trim().uppercase()
                val valueStr = part.substringAfter(":").trim()
                val numVal = valueStr.toFloatOrNull()

                when (key) {
                    "TEMP", "T", "TEMPERATURE" -> if (numVal != null) temp = numVal
                    "HUM", "H", "HUMIDITY" -> if (numVal != null) hum = numVal
                    "GAS", "G", "CO2", "MQ135", "AIR" -> if (numVal != null) gas = numVal
                    "SOIL", "MOISTURE" -> if (numVal != null) soil = numVal
                    "WATER", "LEVEL" -> if (numVal != null) water = numVal
                    "LIGHT", "LUX" -> if (numVal != null) light = numVal
                    "PRODUCT", "RFID", "TAG" -> {
                        rfidTag = valueStr
                        viewModelScope.launch { _scannedRfidFlow.emit(valueStr) }
                    }
                }
            }
        }

        // 3. Fallback: simple float tokens (e.g. "24.5 55.2 120.4")
        if (temp == null && hum == null && tokens.size >= 2) {
            val nums = payload.split(Regex("\\s+")).mapNotNull { it.toFloatOrNull() }
            if (nums.size >= 2) {
                temp = nums[0]
                hum = nums[1]
                if (nums.size >= 3) gas = nums[2]
            }
        }

        // Update telemetry state
        val prev = _latestTelemetry.value
        val updated = prev.copy(
            temperatureC = temp ?: prev.temperatureC,
            humidityPercent = hum ?: prev.humidityPercent,
            gasPpm = gas ?: prev.gasPpm,
            soilMoisturePercent = soil ?: prev.soilMoisturePercent,
            waterLevelPercent = water ?: prev.waterLevelPercent,
            lightLux = light ?: prev.lightLux,
            scannedRfidTag = rfidTag ?: prev.scannedRfidTag,
            rawPayload = payload,
            timestamp = System.currentTimeMillis(),
            packetCount = _packetsReceivedCount.value
        )

        _latestTelemetry.value = updated

        // Maintain telemetry history for charting (max 50 points)
        val history = _telemetryHistory.value.toMutableList()
        history.add(updated)
        if (history.size > 50) history.removeAt(0)
        _telemetryHistory.value = history

        // Auto-save to Room database
        if (_autoSaveToDatabase.value && (temp != null || hum != null || gas != null)) {
            persistReadingToDatabase(
                temp = temp ?: prev.temperatureC,
                hum = hum ?: prev.humidityPercent,
                gas = gas ?: prev.gasPpm
            )
        }
    }

    /**
     * Asynchronously stores incoming telemetry reading in local SQLite/Room database.
     */
    private fun persistReadingToDatabase(temp: Float, hum: Float, gas: Float) {
        val devName = _connectedDevice.value?.name ?: "HC-05 IoT Node"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entity = SensorReadingEntity(
                    deviceName = devName,
                    temperatureC = temp,
                    humidityPercent = hum,
                    co2Ppm = gas,
                    timestamp = System.currentTimeMillis()
                )
                sensorRepository.insertReading(entity)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist sensor reading: ${e.message}")
            }
        }
    }

    /**
     * Send command to the connected HC-05 / BLE node.
     */
    @SuppressLint("MissingPermission")
    fun sendIotCommand(command: String): Boolean {
        val payload = "$command\r\n"
        var sent = false

        // 1. Try Classic SPP Socket
        val socketOut = socketOutputStream
        if (socketOut != null) {
            try {
                socketOut.write(payload.toByteArray())
                socketOut.flush()
                sent = true
                addLog("TX (SPP): $command")
            } catch (e: Exception) {
                Log.e(TAG, "Failed writing command over SPP: ${e.message}")
            }
        }

        // 2. Try BLE GATT write
        val gatt = activeGatt
        if (!sent && gatt != null) {
            try {
                for (service in gatt.services) {
                    for (char in service.characteristics) {
                        val props = char.properties
                        if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                            (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                        ) {
                            @Suppress("DEPRECATION")
                            char.value = payload.toByteArray()
                            @Suppress("DEPRECATION")
                            val res = gatt.writeCharacteristic(char)
                            if (res) {
                                sent = true
                                addLog("TX (GATT): $command")
                                break
                            }
                        }
                    }
                    if (sent) break
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed writing command over GATT: ${e.message}")
            }
        }

        if (sent) {
            _commandsSentCount.value += 1
        } else {
            addLog("Failed sending '$command': Not connected")
        }

        return sent
    }

    /**
     * Disconnects from current device and cleans up connections.
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        dataStreamJob?.cancel()
        dataStreamJob = null

        try {
            socketOutputStream?.close()
            activeSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing socket: ${e.message}")
        } finally {
            socketOutputStream = null
            activeSocket = null
        }

        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing GATT: ${e.message}")
        } finally {
            activeGatt = null
        }

        _connectedDevice.value = null
        _connectionStatus.value = BleDeviceConnectionStatus.Disconnected
        _isCollectingData.value = false
        addLog("Disconnected.")
    }

    fun setFilterHc05Only(enabled: Boolean) {
        _filterHc05Only.value = enabled
    }

    fun setAutoSaveToDatabase(enabled: Boolean) {
        _autoSaveToDatabase.value = enabled
    }

    fun clearDiscoveredDevices() {
        _discoveredMap.clear()
        updateDiscoveredList()
    }

    fun clearLogs() {
        _rawLogStream.value = emptyList()
    }

    /**
     * Simulates real-time IoT reading (for unit testing, emulator verification, or offline demo).
     */
    fun simulateTelemetryReading(
        temp: Float = (220..280).random() / 10f,
        hum: Float = (450..650).random() / 10f,
        gas: Float = (380..520).random().toFloat()
    ) {
        val payload = "TEMP:$temp,HUM:$hum,GAS:$gas,SOIL:62.5,LIGHT:450"
        processIncomingTelemetryString(payload)
    }

    private fun updateTelemetry(transform: RealTimeIotTelemetry.() -> RealTimeIotTelemetry) {
        val current = _latestTelemetry.value
        val updated = current.transform().copy(
            timestamp = System.currentTimeMillis(),
            packetCount = _packetsReceivedCount.value + 1
        )
        _latestTelemetry.value = updated

        val history = _telemetryHistory.value.toMutableList()
        history.add(updated)
        if (history.size > 50) history.removeAt(0)
        _telemetryHistory.value = history
    }

    private fun updateDiscoveredList() {
        _discoveredDevices.value = _discoveredMap.values.sortedByDescending { it.rssi }
    }

    private fun addLog(message: String) {
        val list = _rawLogStream.value.toMutableList()
        list.add(0, "[${formatTime(System.currentTimeMillis())}] $message")
        if (list.size > 80) list.removeAt(list.size - 1)
        _rawLogStream.value = list
    }

    private fun formatTime(ms: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ms))
    }

    private fun checkIfHc05(name: String, address: String): Boolean {
        val upper = name.uppercase()
        return upper.contains("HC-05") ||
                upper.contains("HC05") ||
                upper.contains("HC-06") ||
                upper.contains("HC06") ||
                upper.contains("HC-08") ||
                upper.contains("HM-10") ||
                upper.contains("ECO-NODE") ||
                upper.contains("ARDUINO")
    }

    private fun determineCategory(name: String, serviceUuids: List<String>): BleDeviceCategory {
        val upper = name.uppercase()
        return when {
            upper.contains("HC-05") || upper.contains("HC05") -> BleDeviceCategory.HC05_MODULE
            upper.contains("HC-06") || upper.contains("HC06") -> BleDeviceCategory.HC06_MODULE
            upper.contains("HC-08") -> BleDeviceCategory.HC08_BLE
            upper.contains("HM-10") || upper.contains("HM10") -> BleDeviceCategory.HM10_BLE
            upper.contains("ESP32") -> BleDeviceCategory.ESP32_BLE
            upper.contains("ARDUINO") || upper.contains("NANO") -> BleDeviceCategory.ARDUINO_BLE
            serviceUuids.any { it.contains("181A", ignoreCase = true) } -> BleDeviceCategory.ENVIRONMENTAL_SENSOR
            else -> BleDeviceCategory.GENERIC_BLE
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopBleScan()
        disconnect()
    }
}

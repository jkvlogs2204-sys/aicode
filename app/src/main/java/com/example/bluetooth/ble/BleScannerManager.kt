package com.example.bluetooth.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class BleScannerManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _scanState = MutableStateFlow<BleScanState>(BleScanState.Idle)
    val scanState: StateFlow<BleScanState> = _scanState.asStateFlow()

    private val _discoveredDevicesMap = mutableMapOf<String, BleDevice>()
    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    private val _gattState = MutableStateFlow<BleGattState>(BleGattState.Disconnected)
    val gattState: StateFlow<BleGattState> = _gattState.asStateFlow()

    private val _latestSensorData = MutableStateFlow<BleSensorData?>(null)
    val latestSensorData: StateFlow<BleSensorData?> = _latestSensorData.asStateFlow()

    private val _scannedRfidFlow = MutableSharedFlow<String>()
    val scannedRfidFlow: SharedFlow<String> = _scannedRfidFlow.asSharedFlow()

    private var activeGatt: BluetoothGatt? = null
    private var isScanningHardware = false
    private var connectedBleDevice: BleDevice? = null

    private var sensorStreamJob: Job? = null

    val isBleSupported: Boolean
        get() = true

    val isBleEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled ?: true

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { res ->
                val device = res.device ?: return
                val address = device.address ?: return
                val name = device.name ?: res.scanRecord?.deviceName ?: "BLE Peripheral ($address)"
                val rssi = res.rssi
                val serviceUuids = res.scanRecord?.serviceUuids?.map { it.toString() } ?: emptyList()

                val bleDev = BleDevice(
                    name = name,
                    address = address,
                    rssi = rssi,
                    serviceUuids = serviceUuids,
                    deviceType = determineDeviceType(name, serviceUuids),
                    isEnvironmentalSensor = isEnvironmentalDevice(name, serviceUuids),
                    sensorData = _discoveredDevicesMap[address]?.sensorData,
                    lastSeenMs = System.currentTimeMillis()
                )

                _discoveredDevicesMap[address] = bleDev
                updateDiscoveredList()
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { onScanResult(0, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleScannerManager", "Hardware BLE scan failed with error code $errorCode")
            _scanState.value = BleScanState.Error("BLE hardware scan failed (Error $errorCode)")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val devName = connectedBleDevice?.name ?: gatt?.device?.name ?: "BLE Device"
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d("BleScannerManager", "GATT Connected to $devName. Discovering services...")
                        _gattState.value = BleGattState.Connecting(devName)
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d("BleScannerManager", "GATT Disconnected from $devName")
                        closeGatt()
                        _gattState.value = BleGattState.Disconnected
                    }
                }
            } else {
                Log.e("BleScannerManager", "GATT Connection Error: status $status")
                closeGatt()
                _gattState.value = BleGattState.Error("GATT connection failed (Status $status)")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val servicesList = gatt.services.map { "${it.uuid} (${getServiceName(it.uuid)})" }
                val charList = mutableListOf<String>()

                gatt.services.forEach { service ->
                    service.characteristics.forEach { characteristic ->
                        charList.add("${characteristic.uuid}")
                        // Enable notifications if supported
                        enableNotificationIfSupported(gatt, characteristic)
                    }
                }

                val currentDev = connectedBleDevice ?: BleDevice(
                    name = gatt.device.name ?: "Arduino BLE Node",
                    address = gatt.device.address,
                    rssi = -60
                )

                _gattState.value = BleGattState.Connected(
                    device = currentDev,
                    discoveredServices = servicesList,
                    activeCharacteristics = charList
                )
            } else {
                _gattState.value = BleGattState.Error("Service discovery failed (Status $status)")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                parseCharacteristicValue(characteristic)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic != null) {
                parseCharacteristicValue(characteristic)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startBleScan() {
        if (_scanState.value is BleScanState.Scanning) {
            stopBleScan()
            return
        }

        _discoveredDevicesMap.clear()
        updateDiscoveredList()

        _scanState.value = BleScanState.Scanning(_discoveredDevicesMap.size)

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner != null && bluetoothAdapter?.isEnabled == true) {
            try {
                scanner.startScan(scanCallback)
                isScanningHardware = true
                Log.d("BleScannerManager", "Started real hardware BLE scan")
            } catch (e: Exception) {
                Log.e("BleScannerManager", "Hardware BLE scan failed: ${e.localizedMessage}")
                _scanState.value = BleScanState.Error("BLE scan failed: ${e.localizedMessage}")
            }
        } else {
            _scanState.value = BleScanState.Error("Bluetooth is turned off or BLE scanner unavailable")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        if (isScanningHardware) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.e("BleScannerManager", "Error stopping hardware scan", e)
            }
            isScanningHardware = false
        }

        val count = _discoveredDevicesMap.size
        _scanState.value = BleScanState.Stopped(count)
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BleDevice) {
        stopBleScan()

        connectedBleDevice = device
        _gattState.value = BleGattState.Connecting(device.name)

        closeGatt()

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            try {
                val remoteDevice: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(device.address)
                if (remoteDevice != null) {
                    activeGatt = remoteDevice.connectGatt(context, false, gattCallback)
                    Log.d("BleScannerManager", "Initiated real GATT connection to ${device.address}")
                } else {
                    _gattState.value = BleGattState.Error("Device ${device.address} not reachable")
                }
            } catch (e: Exception) {
                Log.w("BleScannerManager", "GATT hardware connection info: ${e.localizedMessage}")
                _gattState.value = BleGattState.Error("Connection failed: ${e.localizedMessage}")
            }
        } else {
            _gattState.value = BleGattState.Error("Bluetooth is disabled")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectGatt() {
        sensorStreamJob?.cancel()
        sensorStreamJob = null
        closeGatt()
        _gattState.value = BleGattState.Disconnected
    }

    @SuppressLint("MissingPermission")
    fun writeCommandGatt(command: String): Boolean {
        val gatt = activeGatt ?: return false
        val currentState = _gattState.value
        if (currentState !is BleGattState.Connected) return false

        try {
            val bytes = "$command\n".toByteArray()
            // Find writable characteristic (HM-10 FFE1, Nordic RX 6E400002, or any WRITE property)
            for (service in gatt.services) {
                for (char in service.characteristics) {
                    val props = char.properties
                    if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                        (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                    ) {
                        char.value = bytes
                        val result = gatt.writeCharacteristic(char)
                        Log.d("BleScannerManager", "Write command '$command' to ${char.uuid}: $result")
                        return result
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BleScannerManager", "Error writing command over GATT", e)
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun enableNotificationIfSupported(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        val props = characteristic.properties
        if ((props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
            (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
        ) {
            gatt.setCharacteristicNotification(characteristic, true)
            // Write CCCD descriptor 0x2902
            val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    private fun parseCharacteristicValue(characteristic: BluetoothGattCharacteristic) {
        val bytes = characteristic.value ?: return
        val uuidStr = characteristic.uuid.toString().uppercase()

        when {
            uuidStr.contains("2A6E") -> { // Temperature
                val tempFloat = if (bytes.size >= 2) {
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short / 100f
                } else if (bytes.isNotEmpty()) {
                    bytes[0].toFloat()
                } else 0f

                updateSensorData { copy(temperatureC = tempFloat) }
            }
            uuidStr.contains("2A6F") -> { // Humidity
                val humFloat = if (bytes.size >= 2) {
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short / 100f
                } else if (bytes.isNotEmpty()) {
                    bytes[0].toFloat()
                } else 0f

                updateSensorData { copy(humidityPercent = humFloat) }
            }
            else -> { // UART / RFID / Raw text stream
                val payload = String(bytes).trim()
                if (payload.isNotEmpty()) {
                    Log.d("BleScannerManager", "Received raw GATT stream: $payload")
                    val tagId = extractTagId(payload)
                    if (tagId != null) {
                        updateSensorData { copy(scannedRfidTag = tagId) }
                        scope.launch {
                            _scannedRfidFlow.emit(tagId)
                        }
                    }
                }
            }
        }
    }

    private fun updateSensorData(transform: BleSensorData.() -> BleSensorData) {
        val current = _latestSensorData.value ?: BleSensorData()
        _latestSensorData.value = current.transform().copy(lastUpdatedMs = System.currentTimeMillis())
    }

    private fun extractTagId(text: String): String? {
        val upper = text.uppercase()
        return when {
            upper.startsWith("PRODUCT:") -> upper.removePrefix("PRODUCT:").trim()
            upper.startsWith("RFID:") -> upper.removePrefix("RFID:").trim()
            upper.matches(Regex("^[0-9A-Z]{3,12}$")) -> upper.trim()
            else -> null
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            activeGatt = null
            connectedBleDevice = null
        }
    }

    private fun updateDiscoveredList() {
        _discoveredDevices.value = _discoveredDevicesMap.values
            .sortedByDescending { it.rssi }
    }

    private fun determineDeviceType(name: String, serviceUuids: List<String>): String {
        val upperName = name.uppercase()
        return when {
            upperName.contains("ESP32") -> "ESP32 BLE Node"
            upperName.contains("NANO") || upperName.contains("ARDUINO") -> "Arduino BLE Board"
            upperName.contains("HM-10") || upperName.contains("HM10") -> "HM-10 UART BLE"
            upperName.contains("FEATHER") || upperName.contains("ADAFRUIT") -> "Adafruit BLE Sensor"
            serviceUuids.any { it.contains("181a", ignoreCase = true) } -> "Environmental Sensor"
            else -> "BLE Peripheral"
        }
    }

    private fun isEnvironmentalDevice(name: String, serviceUuids: List<String>): Boolean {
        val upperName = name.uppercase()
        return serviceUuids.any { it.contains("181a", ignoreCase = true) } ||
                upperName.contains("ECO") ||
                upperName.contains("SENSOR") ||
                upperName.contains("ARDUINO") ||
                upperName.contains("ESP32")
    }

    private fun getServiceName(uuid: UUID): String {
        val uuidStr = uuid.toString().uppercase()
        return when {
            uuidStr.contains("181A") -> "Environmental Sensing"
            uuidStr.contains("FFE0") -> "HM-10 Serial"
            uuidStr.contains("6E400001") -> "Nordic UART"
            uuidStr.contains("180F") -> "Battery"
            uuidStr.contains("1800") -> "Generic Access"
            else -> "GATT Service"
        }
    }
}

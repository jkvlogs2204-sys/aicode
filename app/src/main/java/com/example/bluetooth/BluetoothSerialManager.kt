package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

sealed class BluetoothState {
    object Disconnected : BluetoothState()
    data class Connecting(val deviceName: String) : BluetoothState()
    data class Reconnecting(val deviceName: String, val attempt: Int, val maxAttempts: Int = 3) : BluetoothState()
    data class Connected(val deviceName: String, val deviceAddress: String) : BluetoothState()
    data class Error(val message: String) : BluetoothState()
}

/**
 * Utility manager for Classic Bluetooth connections using Android's BluetoothAdapter API.
 * Handles device discovery, RFCOMM/SPP socket connections, data stream reading,
 * and command transmissions for Arduino microcontrollers (HC-05, HC-06, ESP32 Bluetooth Classic).
 */
class BluetoothSerialManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private var lastDevice: BluetoothDevice? = null
    private var isUserInitiatedDisconnect = false
    private var maxRetryAttempts = 3
    private var retryCount = 0
    private var reconnectJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionState = MutableStateFlow<BluetoothState>(BluetoothState.Disconnected)
    val connectionState: StateFlow<BluetoothState> = _connectionState.asStateFlow()

    private val _isDiscovering = MutableStateFlow<Boolean>(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveredClassicDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredClassicDevices: StateFlow<List<BluetoothDevice>> = _discoveredClassicDevices.asStateFlow()

    private val _scannedProductIdFlow = MutableSharedFlow<String>()
    val scannedProductIdFlow: SharedFlow<String> = _scannedProductIdFlow.asSharedFlow()

    private val _lastReceivedRawData = MutableStateFlow<String>("")
    val lastReceivedRawData: StateFlow<String> = _lastReceivedRawData.asStateFlow()

    private var isReceiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { dev ->
                        val name = dev.name ?: "Unknown Bluetooth Device"
                        Log.d("BluetoothManager", "Discovered classic device: $name (${dev.address})")
                        val currentList = _discoveredClassicDevices.value.toMutableList()
                        if (currentList.none { it.address == dev.address }) {
                            currentList.add(dev)
                            _discoveredClassicDevices.value = currentList
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("BluetoothManager", "Classic Bluetooth discovery finished.")
                    _isDiscovering.value = false
                    unregisterReceiverIfNeeded()
                }
            }
        }
    }

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    private val prefs = context.getSharedPreferences("ecomind_bluetooth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_LAST_HC05_MAC = "last_hc05_mac"
        private const val PREF_LAST_HC05_NAME = "last_hc05_name"
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    /**
     * Retrieve paired Classic Bluetooth devices, prioritizing HC-05/HC-06 modules.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!isBluetoothSupported || !isBluetoothEnabled) return emptyList()
        return try {
            val list = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            // Prioritize HC-05, HC-06, Arduino devices
            list.sortedByDescending { dev ->
                val name = dev.name ?: ""
                val upper = name.uppercase()
                when {
                    upper.contains("HC-05") || upper.contains("HC05") -> 3
                    upper.contains("HC-06") || upper.contains("HC06") || upper.contains("BT05") -> 2
                    upper.contains("ARDUINO") || upper.contains("ECOMIND") -> 1
                    else -> 0
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getLastConnectedDevice(): Pair<String, String>? {
        val mac = prefs.getString(PREF_LAST_HC05_MAC, null) ?: return null
        val name = prefs.getString(PREF_LAST_HC05_NAME, "HC-05 Module") ?: "HC-05 Module"
        return Pair(mac, name)
    }

    private fun saveLastConnectedDevice(mac: String, name: String) {
        prefs.edit()
            .putString(PREF_LAST_HC05_MAC, mac)
            .putString(PREF_LAST_HC05_NAME, name)
            .apply()
    }

    @SuppressLint("MissingPermission")
    fun autoConnectLastDevice(): Boolean {
        val (mac, _) = getLastConnectedDevice() ?: return false
        connectByAddress(mac)
        return true
    }

    @SuppressLint("MissingPermission")
    fun pairDevice(device: BluetoothDevice): Boolean {
        return try {
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                connectToDevice(device)
                true
            } else {
                val bonding = device.createBond()
                Log.d("BluetoothManager", "Initiated pairing with ${device.address}: $bonding")
                bonding
            }
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Failed to initiate pairing", e)
            false
        }
    }

    /**
     * Start discovering nearby Classic Bluetooth Arduino devices.
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        if (!isBluetoothSupported || !isBluetoothEnabled) {
            _connectionState.value = BluetoothState.Error("Bluetooth is disabled or not supported on this device.")
            return false
        }

        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }

            _discoveredClassicDevices.value = emptyList()
            registerReceiverIfNeeded()

            val started = bluetoothAdapter?.startDiscovery() == true
            _isDiscovering.value = started
            Log.d("BluetoothManager", "Started Classic Bluetooth discovery: $started")
            return started
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Failed to start discovery", e)
            _connectionState.value = BluetoothState.Error("Discovery error: ${e.localizedMessage}")
            _isDiscovering.value = false
            return false
        }
    }

    /**
     * Stop active Bluetooth discovery.
     */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Error stopping discovery", e)
        } finally {
            _isDiscovering.value = false
            unregisterReceiverIfNeeded()
        }
    }

    /**
     * Establish RFCOMM/SPP socket connection to an Arduino device with auto-retry support.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice, isRetry: Boolean = false) {
        if (!isRetry) {
            isUserInitiatedDisconnect = false
            lastDevice = device
            retryCount = 0
            reconnectJob?.cancel()
            reconnectJob = null
        }

        stopDiscovery()
        scope.launch {
            val devName = device.name ?: "HC-05 (${device.address})"
            if (isRetry) {
                _connectionState.value = BluetoothState.Reconnecting(devName, retryCount, maxRetryAttempts)
            } else {
                _connectionState.value = BluetoothState.Connecting(devName)
            }

            try {
                bluetoothAdapter?.cancelDiscovery()

                // Strategy 1: Standard SPP UUID (00001101-0000-1000-8000-00805F9B34FB)
                var newSocket: BluetoothSocket? = null
                var connectException: Exception? = null

                try {
                    Log.d("BluetoothManager", "Attempting standard SPP connection to $devName...")
                    newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    newSocket.connect()
                    Log.d("BluetoothManager", "Connected via standard SPP UUID.")
                } catch (e1: Exception) {
                    Log.w("BluetoothManager", "Strategy 1 (Standard SPP) failed: ${e1.message}. Attempting Strategy 2 (Direct Channel 1 Reflection)...")
                    connectException = e1
                    try {
                        newSocket?.close()
                    } catch (_: Exception) {}

                    // Strategy 2: Direct RFCOMM Channel 1 via Reflection (Fix for HC-05 clones and Android SDP cache bugs)
                    try {
                        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        val fallbackSocket = method.invoke(device, 1) as BluetoothSocket
                        fallbackSocket.connect()
                        newSocket = fallbackSocket
                        Log.d("BluetoothManager", "Connected via direct RFCOMM Channel 1 reflection!")
                    } catch (e2: Exception) {
                        Log.w("BluetoothManager", "Strategy 2 (Reflection) failed: ${e2.message}. Attempting Strategy 3 (Insecure RFCOMM)...")
                        connectException = e2
                        try {
                            newSocket?.close()
                        } catch (_: Exception) {}

                        // Strategy 3: Insecure RFCOMM socket
                        try {
                            val insecureSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                            insecureSocket.connect()
                            newSocket = insecureSocket
                            Log.d("BluetoothManager", "Connected via Insecure RFCOMM.")
                        } catch (e3: Exception) {
                            Log.e("BluetoothManager", "All 3 Bluetooth connection strategies failed for $devName.", e3)
                            throw connectException ?: e3
                        }
                    }
                }

                val establishedSocket = newSocket ?: throw Exception("Could not allocate RFCOMM socket")
                socket = establishedSocket
                outputStream = establishedSocket.outputStream

                retryCount = 0
                saveLastConnectedDevice(device.address, devName)

                _connectionState.value = BluetoothState.Connected(
                    deviceName = devName,
                    deviceAddress = device.address
                )

                // Listen continuously for incoming serial data stream
                listenForSerialData(establishedSocket, device)
            } catch (e: Exception) {
                Log.e("BluetoothManager", "Connection error (Attempt ${if (isRetry) retryCount else 1})", e)
                closeSocket()
                val friendlyError = if (e.localizedMessage?.contains("read failed") == true || e.localizedMessage?.contains("socket might closed") == true) {
                    "HC-05 connection dropped or refused. Ensure device is powered (rapid blinking LED) and paired (PIN 1234 or 0000)."
                } else {
                    e.localizedMessage ?: "Device unreachable or signal lost"
                }
                handleConnectionLoss(device, friendlyError)
            }
        }
    }

    /**
     * Connect to device by MAC address.
     */
    @SuppressLint("MissingPermission")
    fun connectByAddress(macAddress: String) {
        val adapter = bluetoothAdapter ?: run {
            _connectionState.value = BluetoothState.Error("Bluetooth adapter is not available.")
            return
        }
        val cleanMac = macAddress.trim().uppercase()
        if (!BluetoothAdapter.checkBluetoothAddress(cleanMac)) {
            _connectionState.value = BluetoothState.Error("Invalid Bluetooth MAC address format: '$cleanMac'. Expected format: XX:XX:XX:XX:XX:XX")
            return
        }
        try {
            val device = adapter.getRemoteDevice(cleanMac)
            connectToDevice(device)
        } catch (e: Exception) {
            _connectionState.value = BluetoothState.Error("Could not initialize device with MAC $cleanMac: ${e.localizedMessage}")
        }
    }

    private suspend fun listenForSerialData(activeSocket: BluetoothSocket, device: BluetoothDevice) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(activeSocket.inputStream))
            var line: String? = null
            while (activeSocket.isConnected && reader.readLine().also { line = it } != null) {
                val cleanLine = line?.trim() ?: continue
                if (cleanLine.isNotEmpty()) {
                    _lastReceivedRawData.value = cleanLine
                    parseSerialMessage(cleanLine)
                }
            }
            if (!isUserInitiatedDisconnect) {
                Log.w("BluetoothManager", "Serial read loop ended unexpectedly (Signal lost)")
                closeSocket()
                handleConnectionLoss(device, "Signal Lost")
            }
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Serial read loop error or signal lost", e)
            closeSocket()
            if (!isUserInitiatedDisconnect) {
                handleConnectionLoss(device, e.localizedMessage ?: "Signal Lost")
            }
        }
    }

    private fun handleConnectionLoss(device: BluetoothDevice, reason: String) {
        if (isUserInitiatedDisconnect) {
            _connectionState.value = BluetoothState.Disconnected
            return
        }

        if (retryCount < maxRetryAttempts) {
            retryCount++
            val devName = device.name ?: "Arduino Module (${device.address})"
            Log.d("BluetoothManager", "Lost signal to $devName. Scheduling retry attempt $retryCount/$maxRetryAttempts in 2s...")
            _connectionState.value = BluetoothState.Reconnecting(devName, retryCount, maxRetryAttempts)

            reconnectJob?.cancel()
            reconnectJob = scope.launch {
                kotlinx.coroutines.delay(2000L)
                if (!isUserInitiatedDisconnect) {
                    connectToDevice(device, isRetry = true)
                }
            }
        } else {
            val devName = device.name ?: "Arduino Module (${device.address})"
            Log.e("BluetoothManager", "Automatic reconnection failed after $maxRetryAttempts attempts to $devName.")
            _connectionState.value = BluetoothState.Error("Connection lost to $devName ($reason). Auto-reconnect failed after $maxRetryAttempts attempts.")
        }
    }

    fun retryConnection() {
        val dev = lastDevice ?: return
        isUserInitiatedDisconnect = false
        retryCount = 0
        connectToDevice(dev, isRetry = false)
    }

    private suspend fun parseSerialMessage(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return

        val upper = trimmed.uppercase()
        val cleaned = upper
            .replace("PRODUCT:", "")
            .replace("RFID:", "")
            .replace("CARD UID:", "")
            .replace("TAG UID:", "")
            .replace("UID:", "")
            .replace("TAG:", "")
            .replace("CARD:", "")
            .replace("SCAN:", "")
            .trim()

        val sanitized = if (cleaned.contains(":") || cleaned.contains("-") || cleaned.contains(" ")) {
            cleaned.replace(":", "").replace("-", "").replace(" ", "").trim()
        } else {
            cleaned
        }

        val productId = when {
            sanitized.length in 2..32 && sanitized.matches(Regex("^[0-9A-Z_]+$")) -> sanitized
            else -> null
        }

        if (!productId.isNullOrEmpty()) {
            _scannedProductIdFlow.emit(productId)
        }
    }

    // Unidirectional telemetry: The HC-05 Bluetooth connection is strictly used for
    // receiving RFID UID transmissions from Arduino. Reverse decision communication is permanently disabled.

    fun manualRfidScan(productId: String) {
        scope.launch {
            _lastReceivedRawData.value = "PRODUCT:$productId [Manual Input]"
            _scannedProductIdFlow.emit(productId)
        }
    }

    fun disconnect() {
        isUserInitiatedDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        scope.launch {
            stopDiscovery()
            closeSocket()
            _connectionState.value = BluetoothState.Disconnected
        }
    }

    private fun registerReceiverIfNeeded() {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(discoveryReceiver, filter)
                }
                isReceiverRegistered = true
            } catch (e: Exception) {
                Log.e("BluetoothManager", "Error registering receiver", e)
            }
        }
    }

    private fun unregisterReceiverIfNeeded() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {
                Log.e("BluetoothManager", "Error unregistering receiver", e)
            } finally {
                isReceiverRegistered = false
            }
        }
    }

    private fun closeSocket() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputStream = null
            socket = null
        }
    }
}

package com.example.bluetooth.ble

import java.util.UUID

data class BleSensorData(
    val temperatureC: Float = 22.5f,
    val humidityPercent: Float = 45.0f,
    val co2Ppm: Int = 410,
    val scannedRfidTag: String? = null,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val serviceUuids: List<String> = emptyList(),
    val deviceType: String = "Arduino BLE Sensor",
    val isEnvironmentalSensor: Boolean = true,
    val sensorData: BleSensorData? = null,
    val lastSeenMs: Long = System.currentTimeMillis()
) {
    val proximity: String
        get() = when {
            rssi >= -55 -> "Immediate"
            rssi >= -75 -> "Near"
            else -> "Far"
        }

    val signalQualityPercent: Int
        get() = ((rssi + 100).coerceIn(0, 60) * 100) / 60
}

sealed class BleScanState {
    object Idle : BleScanState()
    data class Scanning(val discoveredCount: Int) : BleScanState()
    data class Stopped(val totalFound: Int) : BleScanState()
    data class Error(val message: String) : BleScanState()
}

sealed class BleGattState {
    object Disconnected : BleGattState()
    data class Connecting(val deviceName: String) : BleGattState()
    data class Connected(
        val device: BleDevice,
        val discoveredServices: List<String>,
        val activeCharacteristics: List<String>
    ) : BleGattState()
    data class Error(val message: String) : BleGattState()
}

// Common GATT UUID Constants for Environmental Sensors & Arduino BLE
object BleGattUuids {
    val ENVIRONMENTAL_SENSING_SERVICE: UUID = UUID.fromString("0000181A-0000-1000-8000-00805F9B34FB")
    val TEMPERATURE_CHARACTERISTIC: UUID = UUID.fromString("00002A6E-0000-1000-8000-00805F9B34FB")
    val HUMIDITY_CHARACTERISTIC: UUID = UUID.fromString("00002A6F-0000-1000-8000-00805F9B34FB")
    
    val HM10_UART_SERVICE: UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
    val HM10_UART_CHARACTERISTIC: UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

    val NORDIC_UART_SERVICE: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val NORDIC_UART_RX_CHARACTERISTIC: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val NORDIC_UART_TX_CHARACTERISTIC: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
}

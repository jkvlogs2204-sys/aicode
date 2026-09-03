package com.example.data

import androidx.annotation.Keep

/**
 * Data model representing real-time sensor readings synced from Cloud Firestore.
 * Matches document structure in 'environmental_data' and 'sensor_logs' Firestore collections.
 */
@Keep
data class SensorData(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rfidTagId: String = "",
    val temperatureC: Float = 22.5f,
    val humidityPercent: Float = 48.0f,
    val co2Ppm: Float = 415.0f,
    val carbonMetricKg: Float = 0.5f,
    val environmentalScore: Int = 85,
    val deviceName: String = "Arduino Eco Node",
    val locationName: String = "Main Green Hub",
    val isAlert: Boolean = false,
    val statusLabel: String = "Optimal"
)

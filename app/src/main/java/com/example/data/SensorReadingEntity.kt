package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_readings")
data class SensorReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceName: String = "Arduino Sensor Node",
    val temperatureC: Float,
    val humidityPercent: Float,
    val co2Ppm: Float,
    val timestamp: Long = System.currentTimeMillis()
)

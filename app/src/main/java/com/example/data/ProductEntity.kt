package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val carbon: String,
    val water: String,
    val ecoScore: Int,
    val recycling: String,
    val impact: String,
    val alternative: String,
    val isEcoFriendly: Boolean
)

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Int = 0,
    val productId: String,
    val productName: String,
    val category: String,
    val ecoScore: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "RFID Bluetooth"
)

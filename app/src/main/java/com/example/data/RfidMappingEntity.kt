package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rfid_mappings")
data class RfidMappingEntity(
    @PrimaryKey val tagId: String,
    val zoneName: String,
    val assignedDevice: String,
    val notes: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

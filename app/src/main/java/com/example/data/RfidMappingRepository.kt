package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RfidMappingRepository(private val dao: RfidMappingDao) {

    val allMappings: Flow<List<RfidMappingEntity>> = dao.getAllMappings()

    fun getMappingForTag(tagId: String): Flow<RfidMappingEntity?> {
        return dao.getMappingForTag(tagId)
    }

    suspend fun saveMapping(mapping: RfidMappingEntity) {
        withContext(Dispatchers.IO) {
            dao.insertMapping(mapping)
        }
    }

    suspend fun saveMappings(mappings: List<RfidMappingEntity>) {
        withContext(Dispatchers.IO) {
            dao.insertMappings(mappings)
        }
    }

    suspend fun deleteMapping(tagId: String) {
        withContext(Dispatchers.IO) {
            dao.deleteMapping(tagId)
        }
    }

    suspend fun seedSampleMappings() {
        withContext(Dispatchers.IO) {
            val samples = listOf(
                RfidMappingEntity(tagId = "1001", zoneName = "Zone A - Hydroponics Facility", assignedDevice = "Arduino Node #1 (DHT22 & RC522)", notes = "Monitors water plastic packaging impact"),
                RfidMappingEntity(tagId = "1002", zoneName = "Zone A - Eco Product Station", assignedDevice = "Arduino Node #1 (DHT22 & RC522)", notes = "Reusable container testing zone"),
                RfidMappingEntity(tagId = "1003", zoneName = "Zone B - Organic Textile Bay", assignedDevice = "HC-05 Serial Module #2", notes = "Monitors humidity & cotton storage"),
                RfidMappingEntity(tagId = "1006", zoneName = "Zone C - Zero-Waste Hub", assignedDevice = "ESP32 BLE Sensor Node", notes = "Bamboo biodegradable items tracking"),
                RfidMappingEntity(tagId = "1007", zoneName = "Zone D - E-Waste Sorting Room", assignedDevice = "Arduino Mega RFID Scanner", notes = "Hazardous battery monitoring zone")
            )
            dao.insertMappings(samples)
        }
    }
}

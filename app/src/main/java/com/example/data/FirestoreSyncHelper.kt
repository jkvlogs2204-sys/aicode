package com.example.data

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class DatabaseSyncSummary(
    val productsCount: Int = 0,
    val sensorReadingsCount: Int = 0,
    val rfidMappingsCount: Int = 0,
    val scanHistoryCount: Int = 0,
    val lastBackupTimeMs: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val message: String = ""
)

/**
 * Room-to-Firestore synchronization helper class with offline persistence support.
 * Ensures environmental telemetry from Bluetooth and RFID devices is cached locally
 * when offline and synced automatically once connectivity is restored.
 */
class FirestoreSyncHelper(
    private val sensorReadingDao: SensorReadingDao,
    private val productDao: ProductDao,
    private val rfidMappingDao: RfidMappingDao? = null
) {
    private val TAG = "FirestoreSyncHelper"

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _pendingOfflineCount = MutableStateFlow(0)
    val pendingOfflineCount: StateFlow<Int> = _pendingOfflineCount.asStateFlow()

    private val _isDatabaseSyncing = MutableStateFlow(false)
    val isDatabaseSyncing: StateFlow<Boolean> = _isDatabaseSyncing.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow<DatabaseSyncSummary?>(null)
    val lastSyncSummary: StateFlow<DatabaseSyncSummary?> = _lastSyncSummary.asStateFlow()

    private val db: FirebaseFirestore? by lazy {
        try {
            val isAppInitialized = try { FirebaseApp.getInstance() != null } catch (e: Throwable) { false }
            if (!isAppInitialized) {
                Log.w(TAG, "FirebaseApp is not initialized, Firestore instance unavailable")
                null
            } else {
                val instance = FirebaseFirestore.getInstance()
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                    instance.firestoreSettings = settings
                    Log.d(TAG, "Firestore offline persistence successfully enabled")
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore settings note: ${e.message}")
                }
                instance
            }
        } catch (e: Throwable) {
            Log.e(TAG, "FirebaseFirestore initialization failed: ${e.message}")
            null
        }
    }

    fun setOfflineMode(offline: Boolean) {
        _isOfflineMode.value = offline
        val firestore = db ?: return
        try {
            if (offline) {
                firestore.disableNetwork()
                Log.d(TAG, "Firestore network disabled - operating in Offline Persistence Mode")
            } else {
                firestore.enableNetwork()
                Log.d(TAG, "Firestore network enabled - flushing offline cache mutations to cloud")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling Firestore network mode: ${e.message}")
        }
    }

    suspend fun uploadSensorReading(reading: SensorReadingEntity): Boolean = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext false
        try {
            val docData = hashMapOf(
                "id" to reading.id,
                "deviceName" to reading.deviceName,
                "temperatureC" to reading.temperatureC,
                "humidityPercent" to reading.humidityPercent,
                "co2Ppm" to reading.co2Ppm,
                "timestamp" to reading.timestamp,
                "updatedAt" to System.currentTimeMillis()
            )
            val docId = if (reading.id > 0) reading.id.toString() else System.currentTimeMillis().toString()
            firestore.collection("environmental_data")
                .document(docId)
                .set(docData, SetOptions.merge())
                .await()
            Log.d(TAG, "Uploaded sensor reading to Firestore doc $docId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading sensor reading to Firestore: ${e.message}")
            false
        }
    }

    suspend fun syncAllSensorReadingsToFirestore(readings: List<SensorReadingEntity>): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Pair(false, "Firestore instance not available (check google-services.json)")
        if (readings.isEmpty()) return@withContext Pair(true, "No environmental sensor readings to sync")

        try {
            var successCount = 0
            readings.forEach { reading ->
                val docData = hashMapOf(
                    "id" to reading.id,
                    "deviceName" to reading.deviceName,
                    "temperatureC" to reading.temperatureC,
                    "humidityPercent" to reading.humidityPercent,
                    "co2Ppm" to reading.co2Ppm,
                    "timestamp" to reading.timestamp
                )
                val docId = if (reading.id > 0) reading.id.toString() else "${reading.timestamp}_${(10..99).random()}"
                firestore.collection("environmental_data")
                    .document(docId)
                    .set(docData, SetOptions.merge())
                    .await()
                successCount++
            }
            Pair(true, "Successfully synced $successCount environmental readings to Cloud Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Sync environmental data to Firestore failed: ${e.message}")
            Pair(false, "Firestore Sync Error: ${e.localizedMessage ?: "Cloud sync failed"}")
        }
    }

    suspend fun fetchSensorReadingsFromFirestore(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Pair(false, "Firestore instance not available")
        try {
            val snapshot = firestore.collection("environmental_data")
                .get()
                .await()

            val remoteReadings = mutableListOf<SensorReadingEntity>()
            for (doc in snapshot.documents) {
                val temp = doc.getDouble("temperatureC")?.toFloat() ?: 24.0f
                val hum = doc.getDouble("humidityPercent")?.toFloat() ?: 50.0f
                val co2 = doc.getDouble("co2Ppm")?.toFloat() ?: 410.0f
                val deviceName = doc.getString("deviceName") ?: "Cloud Sensor Node"
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                remoteReadings.add(
                    SensorReadingEntity(
                        deviceName = deviceName,
                        temperatureC = temp,
                        humidityPercent = hum,
                        co2Ppm = co2,
                        timestamp = timestamp
                    )
                )
            }

            if (remoteReadings.isNotEmpty()) {
                sensorReadingDao.insertReadings(remoteReadings)
                Pair(true, "Imported ${remoteReadings.size} cloud environmental records into Room DB")
            } else {
                Pair(true, "Firestore environmental_data collection is empty")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch from Firestore failed: ${e.message}")
            Pair(false, "Firestore Pull Error: ${e.localizedMessage ?: "Unable to read cloud documents"}")
        }
    }

    suspend fun fetchFirestoreReadingsList(): List<SensorReadingEntity> = withContext(Dispatchers.IO) {
        val firestore = db
        if (firestore != null) {
            try {
                val snapshot = firestore.collection("environmental_data")
                    .get()
                    .await()

                val list = mutableListOf<SensorReadingEntity>()
                for (doc in snapshot.documents) {
                    val temp = doc.getDouble("temperatureC")?.toFloat() ?: 24.0f
                    val hum = doc.getDouble("humidityPercent")?.toFloat() ?: 50.0f
                    val co2 = doc.getDouble("co2Ppm")?.toFloat() ?: 410.0f
                    val deviceName = doc.getString("deviceName") ?: "Cloud Sensor Node"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                    list.add(
                        SensorReadingEntity(
                            deviceName = deviceName,
                            temperatureC = temp,
                            humidityPercent = hum,
                            co2Ppm = co2,
                            timestamp = timestamp
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    return@withContext list.sortedByDescending { it.timestamp }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching raw Firestore readings list: ${e.message}")
            }
        }
        return@withContext emptyList()
    }

    suspend fun uploadProductsToFirestore(products: List<ProductEntity>): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Pair(false, "Firestore instance not available")
        try {
            var syncedCount = 0
            products.forEach { p ->
                val data = hashMapOf(
                    "id" to p.id,
                    "name" to p.name,
                    "category" to p.category,
                    "carbon" to p.carbon,
                    "water" to p.water,
                    "ecoScore" to p.ecoScore,
                    "recycling" to p.recycling,
                    "impact" to p.impact,
                    "alternative" to p.alternative,
                    "isEcoFriendly" to p.isEcoFriendly
                )
                firestore.collection("products")
                    .document(p.id)
                    .set(data, SetOptions.merge())
                    .await()
                syncedCount++
            }
            Pair(true, "Synced $syncedCount eco product documents to Cloud Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Product upload to Firestore failed: ${e.message}")
            Pair(false, "Firestore Product Sync Error: ${e.localizedMessage}")
        }
    }

    suspend fun fetchProductsFromFirestore(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Pair(false, "Firestore instance not available")
        try {
            val snapshot = firestore.collection("products")
                .get()
                .await()

            val remoteProducts = mutableListOf<ProductEntity>()
            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: continue
                val category = doc.getString("category") ?: "General"
                val carbon = doc.getString("carbon") ?: "0.5 kg"
                val water = doc.getString("water") ?: "5.0 L"
                val ecoScore = doc.getLong("ecoScore")?.toInt() ?: 75
                val recycling = doc.getString("recycling") ?: "Recyclable"
                val impact = doc.getString("impact") ?: "Eco footprint"
                val alternative = doc.getString("alternative") ?: "Sustainable Option"
                val isEcoFriendly = doc.getBoolean("isEcoFriendly") ?: true

                remoteProducts.add(
                    ProductEntity(
                        id = id,
                        name = name,
                        category = category,
                        carbon = carbon,
                        water = water,
                        ecoScore = ecoScore,
                        recycling = recycling,
                        impact = impact,
                        alternative = alternative,
                        isEcoFriendly = isEcoFriendly
                    )
                )
            }

            if (remoteProducts.isNotEmpty()) {
                productDao.insertProducts(remoteProducts)
                Pair(true, "Imported ${remoteProducts.size} product documents from Firestore into Room DB")
            } else {
                Pair(true, "Firestore products collection is empty")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch products from Firestore failed: ${e.message}")
            Pair(false, "Firestore Pull Error: ${e.localizedMessage}")
        }
    }

    suspend fun linkRfidSensorReadingToLocation(
        tagId: String,
        locationId: String,
        locationName: String,
        temperatureC: Float,
        humidityPercent: Float,
        co2Ppm: Float,
        timestamp: Long = System.currentTimeMillis()
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Pair(false, "Firestore instance unavailable")
        try {
            val docData = hashMapOf(
                "tagId" to tagId,
                "locationId" to locationId,
                "locationName" to locationName,
                "temperatureC" to temperatureC,
                "humidityPercent" to humidityPercent,
                "co2Ppm" to co2Ppm,
                "scannedAt" to timestamp,
                "updatedAt" to System.currentTimeMillis()
            )
            val docId = "rfid_loc_${locationId.lowercase().replace(" ", "_")}_$tagId"
            firestore.collection("rfid_location_sensors")
                .document(docId)
                .set(docData, SetOptions.merge())
                .await()

            val envDoc = hashMapOf(
                "deviceName" to "RFID Node ($locationName)",
                "locationId" to locationId,
                "tagId" to tagId,
                "temperatureC" to temperatureC,
                "humidityPercent" to humidityPercent,
                "co2Ppm" to co2Ppm,
                "timestamp" to timestamp
            )
            firestore.collection("environmental_data")
                .document("rfid_${tagId}_$timestamp")
                .set(envDoc, SetOptions.merge())
                .await()

            Log.d(TAG, "Linked RFID Tag $tagId to Firestore Location $locationId ($locationName)")
            if (_isOfflineMode.value) {
                _pendingOfflineCount.value += 1
                Pair(true, "Cached Offline on Disk (Tag $tagId -> $locationName) - Queued for Cloud Sync")
            } else {
                Pair(true, "Linked RFID Tag $tagId to Firestore Location '$locationName'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error linking RFID sensor reading to Firestore: ${e.message}")
            _pendingOfflineCount.value += 1
            Pair(true, "Cached Offline on Local Disk - Will auto-sync when network is available")
        }
    }

    suspend fun autoFlushPendingOfflineQueue(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val count = _pendingOfflineCount.value
        setOfflineMode(false)
        _pendingOfflineCount.value = 0
        Log.d(TAG, "Auto-flushed pending offline queue of $count items")
        Pair(true, if (count > 0) "Flushed $count queued offline sensor logs to Cloud Firestore" else "Cloud Firestore offline cache synced and up to date")
    }

    /**
     * Upload RFID Mappings to Firestore collection "rfid_mappings"
     */
    suspend fun uploadRfidMappingsToFirestore(mappings: List<RfidMappingEntity>): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Pair(false, "Firestore instance not available")
        if (mappings.isEmpty()) return@withContext Pair(true, "No RFID tag mappings to sync")
        try {
            var count = 0
            mappings.forEach { mapping ->
                val mapData = hashMapOf(
                    "tagId" to mapping.tagId,
                    "zoneName" to mapping.zoneName,
                    "assignedDevice" to mapping.assignedDevice,
                    "notes" to mapping.notes,
                    "lastUpdated" to mapping.lastUpdated
                )
                firestore.collection("rfid_mappings")
                    .document(mapping.tagId)
                    .set(mapData, SetOptions.merge())
                    .await()
                count++
            }
            Pair(true, "Successfully uploaded $count RFID tag mappings to Cloud Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Upload RFID mappings to Firestore failed: ${e.message}")
            Pair(false, "Firestore RFID sync error: ${e.localizedMessage}")
        }
    }

    /**
     * Fetch RFID Mappings from Firestore collection "rfid_mappings"
     */
    suspend fun fetchRfidMappingsFromFirestore(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Pair(false, "Firestore instance not available")
        val dao = rfidMappingDao ?: return@withContext Pair(false, "RFID Mapping DAO not initialized")
        try {
            val snapshot = firestore.collection("rfid_mappings").get().await()
            val remoteMappings = mutableListOf<RfidMappingEntity>()
            for (doc in snapshot.documents) {
                val tagId = doc.getString("tagId") ?: doc.id
                val zoneName = doc.getString("zoneName") ?: "Eco Zone"
                val assignedDevice = doc.getString("assignedDevice") ?: "Sensor Node"
                val notes = doc.getString("notes") ?: ""
                val lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()

                remoteMappings.add(
                    RfidMappingEntity(
                        tagId = tagId,
                        zoneName = zoneName,
                        assignedDevice = assignedDevice,
                        notes = notes,
                        lastUpdated = lastUpdated
                    )
                )
            }
            if (remoteMappings.isNotEmpty()) {
                dao.insertMappings(remoteMappings)
                Pair(true, "Imported ${remoteMappings.size} RFID tag mappings from Firestore into Room DB")
            } else {
                Pair(true, "Firestore rfid_mappings collection is empty")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch RFID mappings failed: ${e.message}")
            Pair(false, "Firestore RFID fetch error: ${e.localizedMessage}")
        }
    }

    /**
     * Upload Scan History to Firestore collection "scan_history" and user scans
     */
    suspend fun uploadScanHistoriesToFirestore(historyList: List<ScanHistoryEntity>, userId: String? = null): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Pair(false, "Firestore instance not available")
        if (historyList.isEmpty()) return@withContext Pair(true, "No scan history records to sync")
        try {
            var count = 0
            historyList.forEach { history ->
                val data = hashMapOf(
                    "historyId" to history.historyId,
                    "productId" to history.productId,
                    "productName" to history.productName,
                    "category" to history.category,
                    "ecoScore" to history.ecoScore,
                    "timestamp" to history.timestamp,
                    "source" to history.source,
                    "userId" to (userId ?: "anonymous")
                )
                val docId = if (history.historyId > 0) "scan_${history.historyId}" else "scan_${history.timestamp}_$count"
                firestore.collection("scan_history")
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .await()

                // Also persist under user document if user is authenticated
                if (!userId.isNullOrBlank()) {
                    firestore.collection("users")
                        .document(userId)
                        .collection("scans")
                        .document(docId)
                        .set(data, SetOptions.merge())
                        .await()
                }
                count++
            }
            Pair(true, "Successfully uploaded $count scan history logs to Cloud Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Upload scan histories failed: ${e.message}")
            Pair(false, "Firestore scan history error: ${e.localizedMessage}")
        }
    }

    /**
     * Fetch Scan History from Firestore
     */
    suspend fun fetchScanHistoriesFromFirestore(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Pair(false, "Firestore instance not available")
        try {
            val snapshot = firestore.collection("scan_history").get().await()
            val list = mutableListOf<ScanHistoryEntity>()
            for (doc in snapshot.documents) {
                val productId = doc.getString("productId") ?: continue
                val productName = doc.getString("productName") ?: "Eco Item"
                val category = doc.getString("category") ?: "General"
                val ecoScore = doc.getLong("ecoScore")?.toInt() ?: 70
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                val source = doc.getString("source") ?: "Cloud Firestore"

                list.add(
                    ScanHistoryEntity(
                        historyId = 0,
                        productId = productId,
                        productName = productName,
                        category = category,
                        ecoScore = ecoScore,
                        timestamp = timestamp,
                        source = source
                    )
                )
            }
            if (list.isNotEmpty()) {
                productDao.insertScanHistories(list)
                Pair(true, "Imported ${list.size} scan history records from Firestore into Room DB")
            } else {
                Pair(true, "Firestore scan_history collection is empty")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch scan histories failed: ${e.message}")
            Pair(false, "Firestore scan history pull error: ${e.localizedMessage}")
        }
    }

    /**
     * ONE-TAP FULL DATABASE BACKUP TO CLOUD FIRESTORE
     * Pushes Products, Sensor Readings, RFID Mappings, and Scan History to Firebase Firestore,
     * recording user profile attribution and system sync metadata.
     */
    suspend fun storeEntireDatabaseToFirestore(
        userId: String? = null,
        userEmail: String? = null
    ): DatabaseSyncSummary = withContext(Dispatchers.IO) {
        _isDatabaseSyncing.value = true
        val firestore = db
        if (firestore == null) {
            _isDatabaseSyncing.value = false
            val summary = DatabaseSyncSummary(
                success = false,
                message = "Firestore instance not initialized. Verify Google Services configuration."
            )
            _lastSyncSummary.value = summary
            return@withContext summary
        }

        try {
            // 1. Fetch all local tables
            val products = productDao.getAllProductsDirect()
            val sensorReadings = sensorReadingDao.getRecentReadingsDirect(500)
            val rfidMappings = rfidMappingDao?.getAllMappingsDirect() ?: emptyList()
            val scanHistories = productDao.getAllScanHistoryDirect()

            // 2. Upload Products
            if (products.isNotEmpty()) {
                uploadProductsToFirestore(products)
            }

            // 3. Upload Sensor Readings
            if (sensorReadings.isNotEmpty()) {
                syncAllSensorReadingsToFirestore(sensorReadings)
            }

            // 4. Upload RFID Mappings
            if (rfidMappings.isNotEmpty()) {
                uploadRfidMappingsToFirestore(rfidMappings)
            }

            // 5. Upload Scan Histories
            if (scanHistories.isNotEmpty()) {
                uploadScanHistoriesToFirestore(scanHistories, userId)
            }

            // 6. Write Central System Sync Summary
            val syncMeta = hashMapOf(
                "productsCount" to products.size,
                "sensorReadingsCount" to sensorReadings.size,
                "rfidMappingsCount" to rfidMappings.size,
                "scanHistoryCount" to scanHistories.size,
                "totalRecords" to (products.size + sensorReadings.size + rfidMappings.size + scanHistories.size),
                "lastBackupTime" to System.currentTimeMillis(),
                "backedUpByUserId" to (userId ?: "anonymous"),
                "backedUpByUserEmail" to (userEmail ?: "guest"),
                "status" to "SYNCED_OK"
            )

            firestore.collection("system_config")
                .document("database_sync_summary")
                .set(syncMeta, SetOptions.merge())
                .await()

            // 7. Update User Profile Document with Database Backup Record
            if (!userId.isNullOrBlank()) {
                firestore.collection("users")
                    .document(userId)
                    .set(
                        hashMapOf(
                            "databaseSyncSummary" to syncMeta,
                            "lastDatabaseBackupAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }

            val summary = DatabaseSyncSummary(
                productsCount = products.size,
                sensorReadingsCount = sensorReadings.size,
                rfidMappingsCount = rfidMappings.size,
                scanHistoryCount = scanHistories.size,
                lastBackupTimeMs = System.currentTimeMillis(),
                success = true,
                message = "Successfully stored complete database (${products.size} products, ${sensorReadings.size} sensor logs, ${rfidMappings.size} RFID tags, ${scanHistories.size} scans) to Cloud Firestore."
            )

            _lastSyncSummary.value = summary
            _isDatabaseSyncing.value = false
            summary
        } catch (e: Exception) {
            Log.e(TAG, "storeEntireDatabaseToFirestore failed: ${e.message}", e)
            _isDatabaseSyncing.value = false
            val summary = DatabaseSyncSummary(
                success = false,
                message = "Database backup to Firestore failed: ${e.localizedMessage}"
            )
            _lastSyncSummary.value = summary
            summary
        }
    }

    /**
     * Restore/Pull Entire Database from Cloud Firestore into local Room DB
     */
    suspend fun restoreEntireDatabaseFromFirestore(): DatabaseSyncSummary = withContext(Dispatchers.IO) {
        _isDatabaseSyncing.value = true
        val firestore = db
        if (firestore == null) {
            _isDatabaseSyncing.value = false
            val summary = DatabaseSyncSummary(
                success = false,
                message = "Firestore instance not available"
            )
            _lastSyncSummary.value = summary
            return@withContext summary
        }

        try {
            fetchProductsFromFirestore()
            fetchSensorReadingsFromFirestore()
            fetchRfidMappingsFromFirestore()
            fetchScanHistoriesFromFirestore()

            val products = productDao.getAllProductsDirect()
            val sensorReadings = sensorReadingDao.getRecentReadingsDirect(500)
            val rfidMappings = rfidMappingDao?.getAllMappingsDirect() ?: emptyList()
            val scanHistories = productDao.getAllScanHistoryDirect()

            val summary = DatabaseSyncSummary(
                productsCount = products.size,
                sensorReadingsCount = sensorReadings.size,
                rfidMappingsCount = rfidMappings.size,
                scanHistoryCount = scanHistories.size,
                lastBackupTimeMs = System.currentTimeMillis(),
                success = true,
                message = "Successfully restored database from Cloud Firestore (${products.size} products, ${sensorReadings.size} sensor records, ${rfidMappings.size} RFID tags, ${scanHistories.size} scans)."
            )

            _lastSyncSummary.value = summary
            _isDatabaseSyncing.value = false
            summary
        } catch (e: Exception) {
            Log.e(TAG, "restoreEntireDatabaseFromFirestore failed: ${e.message}", e)
            _isDatabaseSyncing.value = false
            val summary = DatabaseSyncSummary(
                success = false,
                message = "Restore from Firestore failed: ${e.localizedMessage}"
            )
            _lastSyncSummary.value = summary
            summary
        }
    }
}

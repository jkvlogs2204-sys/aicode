package com.example.ui

import android.app.Application
import android.util.Log
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiProductAnalysis
import com.example.ai.ChatGptEcoAssistant
import com.example.bluetooth.BluetoothSerialManager
import com.example.bluetooth.BluetoothState
import com.example.bluetooth.ble.BleDevice
import com.example.bluetooth.ble.BleGattState
import com.example.bluetooth.ble.BleScanState
import com.example.bluetooth.ble.BleScannerManager
import com.example.bluetooth.ble.BleSensorData
import com.example.data.EcoDatabase
import com.example.data.FirestoreSyncHelper
import com.example.data.ProductEntity
import com.example.data.ProductRepository
import com.example.data.RfidMappingEntity
import com.example.data.RfidMappingRepository
import com.example.data.ScanHistoryEntity
import com.example.data.SensorReadingEntity
import com.example.data.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SensorTelemetryPoint(
    val timestamp: Long = System.currentTimeMillis(),
    val tempC: Float = 22.5f,
    val humidityPercent: Float = 48.0f,
    val co2Ppm: Float = 415.0f
)

data class SensorCalibrationConfig(
    val deviceId: String = "GLOBAL_DEFAULT",
    val deviceName: String = "Global Default Offset",
    val tempOffsetC: Float = 0.0f,
    val humidityOffsetPercent: Float = 0.0f,
    val co2OffsetPpm: Float = 0.0f
)

class EcoMindViewModel(application: Application) : AndroidViewModel(application) {

    private val db = EcoDatabase.getDatabase(application)
    private val firestoreSyncHelper = FirestoreSyncHelper(db.sensorReadingDao(), db.productDao(), db.rfidMappingDao())
    private val repository = ProductRepository(db.productDao(), firestoreSyncHelper)
    private val sensorRepository = SensorRepository(db.sensorReadingDao(), firestoreSyncHelper)
    private val rfidMappingRepository = RfidMappingRepository(db.rfidMappingDao())
    val bluetoothManager = BluetoothSerialManager(application)
    val bleScannerManager = BleScannerManager(application)
    val nfcRfidScannerManager = com.example.nfc.NfcRfidScannerManager(application, firestoreSyncHelper)

    // Firebase Auth Manager
    val authManager = com.example.auth.FirebaseAuthManager(application)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = authManager.currentUser
    val isAuthLoading: StateFlow<Boolean> = authManager.isAuthLoading
    val authStatusMessage: StateFlow<String?> = authManager.authStatusMessage
    val userProfileData: StateFlow<Map<String, Any>?> = authManager.userProfileData
    val isDatabaseSyncing: StateFlow<Boolean> = firestoreSyncHelper.isDatabaseSyncing
    val lastDatabaseSyncSummary: StateFlow<com.example.data.DatabaseSyncSummary?> = firestoreSyncHelper.lastSyncSummary

    val nfcServiceStatus: StateFlow<com.example.nfc.NfcServiceStatus> = nfcRfidScannerManager.serviceStatus
    val isNfcServiceRunning: StateFlow<Boolean> = nfcRfidScannerManager.isServiceRunning
    val nfcScansHistory: StateFlow<List<com.example.nfc.NfcRfidTagScan>> = nfcRfidScannerManager.scansHistory

    private val _firestoreSyncStatus = MutableStateFlow<String?>("Cloud Firestore Sync Ready")
    val firestoreSyncStatus: StateFlow<String?> = _firestoreSyncStatus.asStateFlow()

    private val _isFirestoreSyncing = MutableStateFlow(false)
    val isFirestoreSyncing: StateFlow<Boolean> = _isFirestoreSyncing.asStateFlow()

    private val _isExportingPdf = MutableStateFlow(false)
    val isExportingPdf: StateFlow<Boolean> = _isExportingPdf.asStateFlow()

    private val _pdfExportStatus = MutableStateFlow<String?>(null)
    val pdfExportStatus: StateFlow<String?> = _pdfExportStatus.asStateFlow()

    private val _lastExportedPdfInfo = MutableStateFlow<Pair<java.io.File, android.net.Uri>?>(null)
    val lastExportedPdfInfo: StateFlow<Pair<java.io.File, android.net.Uri>?> = _lastExportedPdfInfo.asStateFlow()

    // Firebase Cloud Messaging (FCM) Push Notification State
    private val _fcmRegistrationToken = MutableStateFlow<String?>("Initializing FCM Token...")
    val fcmRegistrationToken: StateFlow<String?> = _fcmRegistrationToken.asStateFlow()

    private val _lastPushNotificationAlert = MutableStateFlow<String?>(null)
    val lastPushNotificationAlert: StateFlow<String?> = _lastPushNotificationAlert.asStateFlow()

    private val _criticalAlertCount = MutableStateFlow(0)
    val criticalAlertCount: StateFlow<Int> = _criticalAlertCount.asStateFlow()

    // Sensor Calibration Offsets & Safety Preferences
    private val prefs = application.getSharedPreferences("ecomind_sensor_calibration", android.content.Context.MODE_PRIVATE)

    private val _globalTempOffset = MutableStateFlow(prefs.getFloat("global_temp_offset", 0.0f))
    val globalTempOffset: StateFlow<Float> = _globalTempOffset.asStateFlow()

    private val _globalHumOffset = MutableStateFlow(prefs.getFloat("global_hum_offset", 0.0f))
    val globalHumOffset: StateFlow<Float> = _globalHumOffset.asStateFlow()

    private val _globalCo2Offset = MutableStateFlow(prefs.getFloat("global_co2_offset", 0.0f))
    val globalCo2Offset: StateFlow<Float> = _globalCo2Offset.asStateFlow()

    private val _deviceCalibrations = MutableStateFlow<Map<String, SensorCalibrationConfig>>(emptyMap())
    val deviceCalibrations: StateFlow<Map<String, SensorCalibrationConfig>> = _deviceCalibrations.asStateFlow()

    // Safety Alert Thresholds
    private val _co2ThresholdPpm = MutableStateFlow(prefs.getFloat("co2_threshold_ppm", 1000f))
    val co2ThresholdPpm: StateFlow<Float> = _co2ThresholdPpm.asStateFlow()

    private val _tempThresholdC = MutableStateFlow(prefs.getFloat("temp_threshold_c", 35f))
    val tempThresholdC: StateFlow<Float> = _tempThresholdC.asStateFlow()

    private val _humThresholdPercent = MutableStateFlow(prefs.getFloat("hum_threshold_percent", 85f))
    val humThresholdPercent: StateFlow<Float> = _humThresholdPercent.asStateFlow()

    val isFirestoreOfflineMode: StateFlow<Boolean> = firestoreSyncHelper.isOfflineMode
    val pendingOfflineWritesCount: StateFlow<Int> = firestoreSyncHelper.pendingOfflineCount

    // Environmental Sustainability AI State
    private val _sustainabilityAdvice = MutableStateFlow<com.example.ai.EnvironmentalSustainabilityAdvice?>(null)
    val sustainabilityAdvice: StateFlow<com.example.ai.EnvironmentalSustainabilityAdvice?> = _sustainabilityAdvice.asStateFlow()

    private val _isGeneratingSustainabilityAdvice = MutableStateFlow(false)
    val isGeneratingSustainabilityAdvice: StateFlow<Boolean> = _isGeneratingSustainabilityAdvice.asStateFlow()

    // Room Database RFID Mappings Stream
    val rfidMappings: StateFlow<List<RfidMappingEntity>> = rfidMappingRepository.allMappings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active Scanned RFID Zone & Device mapping info
    private val _activeTagMapping = MutableStateFlow<RfidMappingEntity?>(null)
    val activeTagMapping: StateFlow<RfidMappingEntity?> = _activeTagMapping.asStateFlow()

    // Room Database Sensor Readings Stream
    val roomSensorHistory: StateFlow<List<SensorReadingEntity>> = sensorRepository.recentReadings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // State
    val bluetoothState: StateFlow<BluetoothState> = bluetoothManager.connectionState
    val isClassicDiscovering: StateFlow<Boolean> = bluetoothManager.isDiscovering
    val discoveredClassicDevices: StateFlow<List<BluetoothDevice>> = bluetoothManager.discoveredClassicDevices
    val lastRawData: StateFlow<String> = bluetoothManager.lastReceivedRawData

    val bleScanState: StateFlow<BleScanState> = bleScannerManager.scanState
    val bleDiscoveredDevices: StateFlow<List<BleDevice>> = bleScannerManager.discoveredDevices
    val bleGattState: StateFlow<BleGattState> = bleScannerManager.gattState
    val bleLatestSensorData: StateFlow<BleSensorData?> = combine(
        bleScannerManager.latestSensorData,
        globalTempOffset,
        globalHumOffset,
        globalCo2Offset
    ) { rawData, tOff, hOff, cOff ->
        rawData?.let {
            BleSensorData(
                temperatureC = (it.temperatureC + tOff).coerceIn(-40f, 85f),
                humidityPercent = (it.humidityPercent + hOff).coerceIn(0f, 100f),
                co2Ppm = (it.co2Ppm + cOff.toInt()).coerceAtLeast(0)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _telemetryHistory = MutableStateFlow<List<SensorTelemetryPoint>>(emptyList())
    val telemetryHistory: StateFlow<List<SensorTelemetryPoint>> = _telemetryHistory.asStateFlow()

    private val _scannedProduct = MutableStateFlow<ProductEntity?>(null)
    val scannedProduct: StateFlow<ProductEntity?> = _scannedProduct.asStateFlow()

    private val _aiAnalysis = MutableStateFlow<AiProductAnalysis?>(null)
    val aiAnalysis: StateFlow<AiProductAnalysis?> = _aiAnalysis.asStateFlow()

    private val _isAiLoading = MutableStateFlow<Boolean>(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _isChatGptFetchingProduct = MutableStateFlow(false)
    val isChatGptFetchingProduct: StateFlow<Boolean> = _isChatGptFetchingProduct.asStateFlow()

    private val _chatGptFetchStatus = MutableStateFlow<String?>(null)
    val chatGptFetchStatus: StateFlow<String?> = _chatGptFetchStatus.asStateFlow()

    private val _chatGptFetchedProduct = MutableStateFlow<ProductEntity?>(null)
    val chatGptFetchedProduct: StateFlow<ProductEntity?> = _chatGptFetchedProduct.asStateFlow()

    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String>("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _backendUrl = MutableStateFlow<String>("http://10.0.2.2:3000")
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    private val _isBackendSyncing = MutableStateFlow(false)
    val isBackendSyncing: StateFlow<Boolean> = _isBackendSyncing.asStateFlow()

    private val _backendSyncStatus = MutableStateFlow<String?>("Backend DB Status: Standby")
    val backendSyncStatus: StateFlow<String?> = _backendSyncStatus.asStateFlow()

    private val _autoBackendSyncEnabled = MutableStateFlow(true)
    val autoBackendSyncEnabled: StateFlow<Boolean> = _autoBackendSyncEnabled.asStateFlow()

    private val _isMobileMasterNode = MutableStateFlow(false)
    val isMobileMasterNode: StateFlow<Boolean> = _isMobileMasterNode.asStateFlow()

    private val _connectionPingResult = MutableStateFlow<String?>("Not Tested")
    val connectionPingResult: StateFlow<String?> = _connectionPingResult.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _chatGptConnectionStatus = MutableStateFlow<com.example.ai.ChatGptConnectionTestResult?>(null)
    val chatGptConnectionStatus: StateFlow<com.example.ai.ChatGptConnectionTestResult?> = _chatGptConnectionStatus.asStateFlow()

    private val _isTestingChatGpt = MutableStateFlow(false)
    val isTestingChatGpt: StateFlow<Boolean> = _isTestingChatGpt.asStateFlow()

    private val _chatGptApiKeySource = MutableStateFlow<String>(ChatGptEcoAssistant.getActiveKeySource())
    val chatGptApiKeySource: StateFlow<String> = _chatGptApiKeySource.asStateFlow()

    private val _isChatGptConfigured = MutableStateFlow<Boolean>(ChatGptEcoAssistant.isChatGptConfigured())
    val isChatGptConfigured: StateFlow<Boolean> = _isChatGptConfigured.asStateFlow()

    fun updateChatGptApiKey(newKey: String) {
        ChatGptEcoAssistant.setCustomApiKey(getApplication(), newKey)
        _chatGptApiKeySource.value = ChatGptEcoAssistant.getActiveKeySource()
        _isChatGptConfigured.value = ChatGptEcoAssistant.isChatGptConfigured()
        testChatGptApiLive()
    }

    fun clearChatGptApiKey() {
        ChatGptEcoAssistant.clearCustomApiKey(getApplication())
        _chatGptApiKeySource.value = ChatGptEcoAssistant.getActiveKeySource()
        _isChatGptConfigured.value = ChatGptEcoAssistant.isChatGptConfigured()
        testChatGptApiLive()
    }

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("ai", "Hello! I am Eco Mind Guide powered by AI. Ask me anything about recycling methods, CO₂ footprints, water impact, e-waste guidelines, or your scanned products!")
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _lastArduinoFeedback = MutableStateFlow<String?>("Unidirectional Telemetry: Arduino → Mobile App Active")
    val lastArduinoFeedback: StateFlow<String?> = _lastArduinoFeedback.asStateFlow()

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanHistory: StateFlow<List<ScanHistoryEntity>> = repository.scanHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        searchQuery,
        selectedCategory
    ) { products, query, category ->
        products.filter { p ->
            val matchesCategory = (category == "All" || p.category.equals(category, ignoreCase = true))
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.category.contains(query, ignoreCase = true) ||
                    p.id.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        com.example.fcm.EcoMindMessagingService.fetchFcmToken(getApplication()) { token ->
            _fcmRegistrationToken.value = token ?: "Token unavailable (Check Google Play Services)"
        }

        // Ensure Database is populated with 20 sample products
        viewModelScope.launch {
            repository.ensureSeeded()
            // Set initial product to Plastic Water Bottle (1001) for immediate visual richness
            val defaultProduct = repository.getProductByIdDirect("1001")
            _scannedProduct.value = defaultProduct
            if (defaultProduct != null) {
                generateAiAnalysis(defaultProduct)
            }
        }

        // Initial AI Sustainability Analysis generation
        viewModelScope.launch {
            generateSustainabilityAdviceFromFirestore()
        }

        // Initial ChatGPT AI API verification
        viewModelScope.launch {
            testChatGptApiLive()
        }

        // Listen for incoming Bluetooth serial RFID tag scans
        viewModelScope.launch {
            bluetoothManager.scannedProductIdFlow.collect { productId ->
                onRfidTagScanned(productId)
            }
        }

        // Listen for incoming BLE sensor RFID scans
        viewModelScope.launch {
            bleScannerManager.scannedRfidFlow.collect { productId ->
                onRfidTagScanned(productId)
            }
        }

        // Listen in real-time to Cloud Firestore environmental sensor updates via Repository layer
        viewModelScope.launch {
            sensorRepository.observeRealtimeFirestoreSensorData().collect { realtimeList ->
                Log.d("EcoMindViewModel", "Real-time Firestore sensor update: ${realtimeList.size} readings received from cloud")
            }
        }

        // Seed initial Room sensor database readings if empty
        viewModelScope.launch {
            val existing = roomSensorHistory.firstOrNull()
            if (existing.isNullOrEmpty()) {
                val now = System.currentTimeMillis()
                val initialReadings = listOf(
                    SensorReadingEntity(deviceName = "Arduino Node (DHT22)", temperatureC = 21.5f, humidityPercent = 45f, co2Ppm = 405f, timestamp = now - 3600000 * 5),
                    SensorReadingEntity(deviceName = "Arduino Node (DHT22)", temperatureC = 22.0f, humidityPercent = 47f, co2Ppm = 412f, timestamp = now - 3600000 * 4),
                    SensorReadingEntity(deviceName = "Arduino Node (DHT22)", temperatureC = 23.2f, humidityPercent = 50f, co2Ppm = 425f, timestamp = now - 3600000 * 3),
                    SensorReadingEntity(deviceName = "Arduino Node (DHT22)", temperatureC = 24.1f, humidityPercent = 52f, co2Ppm = 438f, timestamp = now - 3600000 * 2),
                    SensorReadingEntity(deviceName = "Arduino Node (DHT22)", temperatureC = 23.8f, humidityPercent = 49f, co2Ppm = 420f, timestamp = now - 3600000 * 1),
                    SensorReadingEntity(deviceName = "Arduino Node (DHT22)", temperatureC = 24.5f, humidityPercent = 48f, co2Ppm = 418f, timestamp = now)
                )
                sensorRepository.insertReadings(initialReadings)
            }
        }

        // Listen for incoming BLE sensor environmental metrics
        viewModelScope.launch {
            bleLatestSensorData.collect { sensor ->
                sensor?.let {
                    val co2Calculated = 400f + (it.temperatureC * 2f)
                    val currentList = _telemetryHistory.value.toMutableList()
                    val newPoint = SensorTelemetryPoint(
                        timestamp = it.lastUpdatedMs,
                        tempC = it.temperatureC,
                        humidityPercent = it.humidityPercent,
                        co2Ppm = co2Calculated
                    )
                    currentList.add(newPoint)
                    if (currentList.size > 20) {
                        currentList.removeAt(0)
                    }
                    _telemetryHistory.value = currentList

                    // Persist reading directly into Room Database and queue to Firestore
                    val entity = SensorReadingEntity(
                        deviceName = "BLE Sensor Node",
                        temperatureC = it.temperatureC,
                        humidityPercent = it.humidityPercent,
                        co2Ppm = co2Calculated,
                        timestamp = it.lastUpdatedMs
                    )
                    sensorRepository.insertReading(entity)
                    firestoreSyncHelper.uploadSensorReading(entity)
                }
            }
        }

        // Listen for incoming Bluetooth serial stream data for telemetry
        viewModelScope.launch {
            lastRawData.collect { raw ->
                if (raw.contains("TEMP:") || raw.contains("HUM:") || raw.contains("CO2:")) {
                    val temp = extractValue(raw, "TEMP:") ?: 23.5f
                    val hum = extractValue(raw, "HUM:") ?: 52.0f
                    val co2 = extractValue(raw, "CO2:") ?: 410.0f
                    val currentList = _telemetryHistory.value.toMutableList()
                    val now = System.currentTimeMillis()
                    currentList.add(
                        SensorTelemetryPoint(
                            timestamp = now,
                            tempC = temp,
                            humidityPercent = hum,
                            co2Ppm = co2
                        )
                    )
                    if (currentList.size > 20) {
                        currentList.removeAt(0)
                    }
                    _telemetryHistory.value = currentList

                    // Persist reading directly into Room Database
                    sensorRepository.insertReading(
                        SensorReadingEntity(
                            deviceName = "Serial HC-05 Node",
                            temperatureC = temp,
                            humidityPercent = hum,
                            co2Ppm = co2,
                            timestamp = now
                        )
                    )
                }
            }
        }
    }

    private fun extractValue(raw: String, prefix: String): Float? {
        val index = raw.indexOf(prefix)
        if (index == -1) return null
        val sub = raw.substring(index + prefix.length)
        val end = sub.indexOfAny(charArrayOf(',', ' ', ';', '\n'))
        val token = if (end != -1) sub.substring(0, end) else sub
        return token.trim().toFloatOrNull()
    }

    fun updateProduct(product: ProductEntity, syncToBackend: Boolean = true) {
        viewModelScope.launch {
            repository.updateProduct(product)
            _scannedProduct.value = product
            generateAiAnalysis(product)

            if (syncToBackend && _autoBackendSyncEnabled.value) {
                _isBackendSyncing.value = true
                _backendSyncStatus.value = "Updating backend DB for '${product.name}'..."
                val (success, message) = repository.syncProductToBackend(_backendUrl.value, product)
                _isBackendSyncing.value = false
                _backendSyncStatus.value = message
            } else {
                _backendSyncStatus.value = "Updated in local Room DB"
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            if (_scannedProduct.value?.id == productId) {
                _scannedProduct.value = null
            }
            _backendSyncStatus.value = "Deleted product #$productId from local Room DB"
        }
    }

    private var lastScanTimestamp = 0L
    private var lastScannedUid = ""

    fun onRfidTagScanned(productId: String) {
        val cleanUid = productId.trim().uppercase()
        val now = System.currentTimeMillis()
        if (cleanUid == lastScannedUid && (now - lastScanTimestamp) < 2500L) {
            Log.d("EcoMindViewModel", "Duplicate scan suppressed for $cleanUid (2.5s cooldown active)")
            return
        }
        lastScanTimestamp = now
        lastScannedUid = cleanUid

        viewModelScope.launch {
            // Fetch mapped environmental zone / device
            val mapping = rfidMappingRepository.getMappingForTag(cleanUid).firstOrNull()
            _activeTagMapping.value = mapping

            // 1. Check local Room database
            var product = repository.getProductByIdDirect(cleanUid)

            // 2. If not found locally, try remote backend
            if (product == null) {
                product = repository.fetchFromRemoteBackend(_backendUrl.value, cleanUid)
            }

            val scanSource = if (bluetoothState.value is BluetoothState.Connected) "HC-05 Bluetooth" else "RFID Reader"

            if (product != null) {
                _scannedProduct.value = product
                repository.addScanHistory(
                    productId = product.id,
                    productName = product.name,
                    category = product.category,
                    ecoScore = product.ecoScore,
                    source = scanSource
                )
                generateAiAnalysis(product)
            } else {
                val initialEntity = ProductEntity(
                    id = cleanUid,
                    name = "Scanned Tag ($cleanUid)",
                    category = "Plastic",
                    carbon = "120g CO2",
                    water = "4.5 litres",
                    ecoScore = 60,
                    recycling = "Consult local recycling municipal guidelines",
                    impact = "RFID Tag $cleanUid scanned over HC-05. Fetching AI environmental analysis...",
                    alternative = "Reusable or eco-friendly alternative",
                    isEcoFriendly = true
                )
                _scannedProduct.value = initialEntity
                repository.addScanHistory(
                    productId = cleanUid,
                    productName = initialEntity.name,
                    category = initialEntity.category,
                    ecoScore = initialEntity.ecoScore,
                    source = scanSource
                )
                generateAiAnalysis(initialEntity)

                // Query ChatGPT AI in background to fetch detailed environmental specifications
                viewModelScope.launch {
                    try {
                        val aiProduct = ChatGptEcoAssistant.fetchProductDetailsViaChatGpt(cleanUid)
                        val finalProduct = aiProduct.copy(id = cleanUid)
                        repository.updateProduct(finalProduct)
                        _scannedProduct.value = finalProduct
                        generateAiAnalysis(finalProduct)
                        Log.d("EcoMindViewModel", "Auto-registered scanned RFID tag $cleanUid via ChatGPT AI")
                    } catch (e: Exception) {
                        Log.w("EcoMindViewModel", "AI auto-lookup for RFID tag $cleanUid failed: ${e.message}")
                    }
                }
            }
        }
    }

    // RFID Tag & Zone Management Operations
    fun saveRfidMapping(tagId: String, zoneName: String, assignedDevice: String, notes: String) {
        viewModelScope.launch {
            val mapping = RfidMappingEntity(
                tagId = tagId.trim(),
                zoneName = zoneName.trim().ifBlank { "Unassigned Zone" },
                assignedDevice = assignedDevice.trim().ifBlank { "Unassigned Node" },
                notes = notes.trim(),
                lastUpdated = System.currentTimeMillis()
            )
            rfidMappingRepository.saveMapping(mapping)
            if (_scannedProduct.value?.id == tagId.trim()) {
                _activeTagMapping.value = mapping
            }
        }
    }

    fun deleteRfidMapping(tagId: String) {
        viewModelScope.launch {
            rfidMappingRepository.deleteMapping(tagId)
            if (_activeTagMapping.value?.tagId == tagId) {
                _activeTagMapping.value = null
            }
        }
    }

    fun seedDefaultRfidMappings() {
        viewModelScope.launch {
            rfidMappingRepository.seedSampleMappings()
        }
    }

    fun fetchAndCreateProductWithChatGpt(query: String, onResult: ((ProductEntity) -> Unit)? = null) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isChatGptFetchingProduct.value = true
            _chatGptFetchStatus.value = "Connecting to ChatGPT API (gpt-4o-mini): Calculating recycling methods, carbon footprint & water impact for '$query'..."
            val fetched = ChatGptEcoAssistant.fetchProductDetailsViaChatGpt(query)
            
            // Save into local Room DB and Cloud Firestore
            repository.updateProduct(fetched)
            
            _chatGptFetchedProduct.value = fetched
            _scannedProduct.value = fetched
            generateAiAnalysis(fetched)
            
            _isChatGptFetchingProduct.value = false
            _chatGptFetchStatus.value = "Successfully fetched '${fetched.name}' via ChatGPT AI!"
            onResult?.invoke(fetched)
        }
    }

    fun generateAiAnalysis(product: ProductEntity) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val analysis = ChatGptEcoAssistant.analyzeProduct(product)
            _aiAnalysis.value = analysis
            _isAiLoading.value = false
        }
    }

    fun sendArduinoCommand(cmd: String) {
        // Enforce strict unidirectional architecture: No return commands (GREEN/YELLOW/RED)
        // are transmitted to Arduino. Environmental decisions are displayed on-screen only.
        android.util.Log.d("EcoMindViewModel", "Ignored reverse hardware command '$cmd': Communication is strictly Arduino -> App.")
    }

    fun startClassicDiscovery() {
        bluetoothManager.startDiscovery()
    }

    fun stopClassicDiscovery() {
        bluetoothManager.stopDiscovery()
    }

    val isBluetoothEnabled: Boolean
        get() = bluetoothManager.isBluetoothEnabled

    val isBluetoothSupported: Boolean
        get() = bluetoothManager.isBluetoothSupported

    fun getPairedClassicDevices(): List<BluetoothDevice> = bluetoothManager.getPairedDevices()

    fun connectDevice(device: BluetoothDevice) {
        bluetoothManager.connectToDevice(device)
    }

    fun connectByMacAddress(mac: String) {
        bluetoothManager.connectByAddress(mac)
    }

    fun pairAndConnectDevice(device: BluetoothDevice) {
        bluetoothManager.pairDevice(device)
    }

    fun autoConnectSavedHc05(): Boolean {
        return bluetoothManager.autoConnectLastDevice()
    }

    fun getSavedHc05Device(): Pair<String, String>? {
        return bluetoothManager.getLastConnectedDevice()
    }

    fun disconnectDevice() {
        bluetoothManager.disconnect()
    }

    fun retryBluetoothConnection() {
        bluetoothManager.retryConnection()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setBackendUrl(url: String) {
        _backendUrl.value = url
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val currentMsgList = _chatMessages.value.toMutableList()
        currentMsgList.add(ChatMessage("user", userText))
        _chatMessages.value = currentMsgList

        viewModelScope.launch {
            _isChatLoading.value = true
            val historyPairs = currentMsgList.map { Pair(it.sender, it.text) }
            val aiResponse = ChatGptEcoAssistant.askEcoChatHistory(historyPairs, _scannedProduct.value)
            val updatedList = _chatMessages.value.toMutableList()
            updatedList.add(ChatMessage("ai", aiResponse))
            _chatMessages.value = updatedList
            _isChatLoading.value = false
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage("ai", "Hello! I am Eco Mind Guide powered by AI. Ask me anything about recycling methods, CO₂ footprints, water impact, e-waste guidelines, or your scanned products!")
        )
    }

    fun testChatGptApiLive() {
        viewModelScope.launch {
            _isTestingChatGpt.value = true
            val result = ChatGptEcoAssistant.testChatGptConnection()
            _chatGptConnectionStatus.value = result
            _isTestingChatGpt.value = false
            _isChatGptConfigured.value = ChatGptEcoAssistant.isChatGptConfigured()
            _chatGptApiKeySource.value = ChatGptEcoAssistant.getActiveKeySource()
        }
    }

    fun manualRfidScan(productId: String) {
        bluetoothManager.manualRfidScan(productId)
    }

    // BLE Utility Operations
    fun startBleScan() {
        bleScannerManager.startBleScan()
    }

    fun stopBleScan() {
        bleScannerManager.stopBleScan()
    }

    fun connectBleDevice(device: BleDevice) {
        bleScannerManager.connectToDevice(device)
    }

    fun disconnectBleGatt() {
        bleScannerManager.disconnectGatt()
    }

    fun sendBleCommand(cmd: String) {
        // Enforce strict unidirectional architecture: No return commands (GREEN/YELLOW/RED)
        // are transmitted to Arduino. Environmental decisions are displayed on-screen only.
        android.util.Log.d("EcoMindViewModel", "Ignored BLE reverse command '$cmd': Communication is strictly unidirectional.")
    }

    fun triggerBleRfidScan(productId: String) {
        // Directly perform RFID identification scan without sending any hardware write commands
        manualRfidScan(productId)
    }

    // Room Database Sensor Operations
    fun logSensorReadingToRoom(tempC: Float, humPercent: Float, co2Ppm: Float, context: android.content.Context? = null) {
        viewModelScope.launch {
            val entity = SensorReadingEntity(
                deviceName = "Environmental Node",
                temperatureC = tempC,
                humidityPercent = humPercent,
                co2Ppm = co2Ppm
            )
            sensorRepository.insertReading(entity)

            // Trigger FCM push notification if threshold exceeded
            context?.let { ctx ->
                checkAndTriggerCriticalSensorPushNotification(ctx, tempC, humPercent, co2Ppm, "Environmental Node")
            }
        }
    }

    fun clearRoomSensorHistory() {
        viewModelScope.launch {
            sensorRepository.clearHistory()
        }
    }

    // Backend Remote Database Operations
    fun toggleAutoBackendSync(enabled: Boolean) {
        _autoBackendSyncEnabled.value = enabled
        _backendSyncStatus.value = if (enabled) "Auto Backend DB Sync: Enabled" else "Auto Backend DB Sync: Disabled"
    }

    fun toggleMobileMasterNode(enabled: Boolean) {
        _isMobileMasterNode.value = enabled
        _backendSyncStatus.value = if (enabled) {
            "Mobile Master Node Mode Active: Broadcasting Room DB via local endpoint"
        } else {
            "Mobile Master Node Mode Disconnected"
        }
    }

    fun testBackendConnection() {
        viewModelScope.launch {
            _connectionPingResult.value = "Testing connection to ${_backendUrl.value}..."
            val (success, message) = repository.testBackendConnection(_backendUrl.value)
            if (success) {
                _connectionPingResult.value = "SUCCESS: $message"
                _backendSyncStatus.value = "Backend Online & Connected (${_backendUrl.value})"
            } else {
                _connectionPingResult.value = "FAILED: $message"
                _backendSyncStatus.value = "Backend Offline / Unreachable (${_backendUrl.value})"
            }
        }
    }

    fun fetchFullDatabaseFromBackend() {
        viewModelScope.launch {
            _isBackendSyncing.value = true
            _backendSyncStatus.value = "Fetching remote DB from ${_backendUrl.value}..."
            val (success, message) = repository.fetchFullDatabaseFromBackend(_backendUrl.value)
            _isBackendSyncing.value = false
            _backendSyncStatus.value = message
        }
    }

    fun reseedDatabase() {
        viewModelScope.launch {
            _isBackendSyncing.value = true
            _backendSyncStatus.value = "Resetting & Repopulating Room Database..."
            repository.resetDatabaseToDefaults()
            rfidMappingRepository.seedSampleMappings()
            _isBackendSyncing.value = false
            _backendSyncStatus.value = "Room DB Reset Complete: Loaded 20 Default Eco Products & Mappings"
            
            val defaultProduct = repository.getProductByIdDirect("1001")
            _scannedProduct.value = defaultProduct
            if (defaultProduct != null) {
                generateAiAnalysis(defaultProduct)
            }
        }
    }

    fun syncFullDatabaseToBackend() {
        viewModelScope.launch {
            _isBackendSyncing.value = true
            _backendSyncStatus.value = "Syncing Room DB to Backend DB..."
            val productsList = allProducts.value
            val sensorList = roomSensorHistory.value
            val (success, message) = repository.syncFullDatabaseToBackend(
                baseUrl = _backendUrl.value,
                products = productsList,
                sensorReadings = sensorList
            )
            _isBackendSyncing.value = false
            _backendSyncStatus.value = message
        }
    }

    // --- Firebase Firestore Synchronization ---
    fun syncEnvironmentalDataToFirestore() {
        viewModelScope.launch {
            _isFirestoreSyncing.value = true
            _firestoreSyncStatus.value = "Uploading Room environmental readings to Cloud Firestore..."
            val res = sensorRepository.syncToFirestore()
            _isFirestoreSyncing.value = false
            _firestoreSyncStatus.value = res.second
        }
    }

    fun fetchEnvironmentalDataFromFirestore() {
        viewModelScope.launch {
            _isFirestoreSyncing.value = true
            _firestoreSyncStatus.value = "Fetching environmental data from Cloud Firestore..."
            val res = sensorRepository.fetchFromFirestore()
            _isFirestoreSyncing.value = false
            _firestoreSyncStatus.value = res.second
        }
    }

    fun syncProductsToFirestore() {
        viewModelScope.launch {
            _isFirestoreSyncing.value = true
            _firestoreSyncStatus.value = "Pushing Room product database to Cloud Firestore..."
            val res = repository.syncProductsToFirestore()
            _isFirestoreSyncing.value = false
            _firestoreSyncStatus.value = res.second
        }
    }

    fun fetchProductsFromFirestore() {
        viewModelScope.launch {
            _isFirestoreSyncing.value = true
            _firestoreSyncStatus.value = "Pulling eco products from Cloud Firestore..."
            val res = repository.fetchProductsFromFirestore()
            _isFirestoreSyncing.value = false
            _firestoreSyncStatus.value = res.second
        }
    }

    /**
     * ONE-TAP: Store Entire Room Database into Firebase Firestore
     */
    fun storeEntireDatabaseToFirestore() {
        viewModelScope.launch {
            _isFirestoreSyncing.value = true
            _firestoreSyncStatus.value = "Backing up entire local database to Cloud Firestore..."
            val user = currentUser.value
            val summary = firestoreSyncHelper.storeEntireDatabaseToFirestore(
                userId = user?.uid,
                userEmail = user?.email
            )
            _isFirestoreSyncing.value = false
            _firestoreSyncStatus.value = summary.message
        }
    }

    /**
     * Restore Entire Database from Firebase Firestore into local Room DB
     */
    fun restoreEntireDatabaseFromFirestore() {
        viewModelScope.launch {
            _isFirestoreSyncing.value = true
            _firestoreSyncStatus.value = "Restoring complete database from Cloud Firestore..."
            val summary = firestoreSyncHelper.restoreEntireDatabaseFromFirestore()
            _isFirestoreSyncing.value = false
            _firestoreSyncStatus.value = summary.message
        }
    }

    fun syncRfidMappingsToFirestore() {
        viewModelScope.launch {
            _isFirestoreSyncing.value = true
            _firestoreSyncStatus.value = "Uploading RFID tag mappings to Cloud Firestore..."
            val mappings = db.rfidMappingDao().getAllMappingsDirect()
            val res = firestoreSyncHelper.uploadRfidMappingsToFirestore(mappings)
            _isFirestoreSyncing.value = false
            _firestoreSyncStatus.value = res.second
        }
    }

    fun fetchRfidMappingsFromFirestore() {
        viewModelScope.launch {
            _isFirestoreSyncing.value = true
            _firestoreSyncStatus.value = "Fetching RFID tag mappings from Cloud Firestore..."
            val res = firestoreSyncHelper.fetchRfidMappingsFromFirestore()
            _isFirestoreSyncing.value = false
            _firestoreSyncStatus.value = res.second
        }
    }

    // --- Firebase Auth Actions ---
    fun signInWithGoogle(context: android.content.Context) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(context)
            _firestoreSyncStatus.value = result.second
        }
    }

    fun signInOrRegisterWithEmail(email: String, pass: String, displayName: String = "") {
        viewModelScope.launch {
            val result = authManager.signInOrRegisterWithEmail(email, pass, displayName)
            _firestoreSyncStatus.value = result.second
        }
    }

    fun quickSignInVerifiedUser(
        email: String = "jkvlogs2204@gmail.com",
        name: String = "JK Vlogs"
    ) {
        viewModelScope.launch {
            val result = authManager.quickSignInVerifiedUser(email, name)
            _firestoreSyncStatus.value = result.second
        }
    }

    fun signOut() {
        authManager.signOut()
        _firestoreSyncStatus.value = "Signed out of Firebase"
    }

    fun generateSustainabilityAdviceFromFirestore() {
        viewModelScope.launch {
            _isGeneratingSustainabilityAdvice.value = true
            val readings = roomSensorHistory.value
            val bleData = bleLatestSensorData.value
            val advice = ChatGptEcoAssistant.generateEnvironmentalSuggestions(
                readings = readings,
                latestTemp = bleData?.temperatureC,
                latestHum = bleData?.humidityPercent,
                latestCo2 = telemetryHistory.value.lastOrNull()?.co2Ppm
            )
            _sustainabilityAdvice.value = advice
            _isGeneratingSustainabilityAdvice.value = false
        }
    }

    fun toggleFirestoreOfflineMode(offline: Boolean) {
        firestoreSyncHelper.setOfflineMode(offline)
        _firestoreSyncStatus.value = if (offline) {
            "Offline Persistence Mode ACTIVE - RFID & Bluetooth telemetry cached on local disk"
        } else {
            "Cloud Connectivity Restored - Auto-syncing offline persistent cache to Cloud Firestore"
        }
    }

    fun flushOfflineFirestoreQueue() {
        viewModelScope.launch {
            _isFirestoreSyncing.value = true
            _firestoreSyncStatus.value = "Flushing offline persistent disk cache to Cloud Firestore..."
            val res = firestoreSyncHelper.autoFlushPendingOfflineQueue()
            syncEnvironmentalDataToFirestore()
            _isFirestoreSyncing.value = false
            _firestoreSyncStatus.value = res.second
        }
    }

    fun exportFirestoreSensorHistoryToPdf(context: android.content.Context) {
        viewModelScope.launch {
            _isExportingPdf.value = true
            _pdfExportStatus.value = "Fetching sensor readings from Firestore..."

            var readings = firestoreSyncHelper.fetchFirestoreReadingsList()
            if (readings.isEmpty()) {
                readings = roomSensorHistory.value
            }

            if (readings.isEmpty()) {
                _isExportingPdf.value = false
                _pdfExportStatus.value = "No sensor telemetry records available to export."
                return@launch
            }

            _pdfExportStatus.value = "Formatting and rendering PDF report (${readings.size} records)..."
            val pdfResult = com.example.util.PdfReportGenerator.generateSensorPdfReport(
                context = context,
                readings = readings,
                isFirestoreDataSource = true
            )

            _isExportingPdf.value = false
            if (pdfResult != null) {
                _lastExportedPdfInfo.value = pdfResult
                _pdfExportStatus.value = "PDF Exported: ${pdfResult.first.name} (${readings.size} logs)"
                com.example.util.PdfReportGenerator.sharePdfReportViaIntent(context, pdfResult.second, pdfResult.first)
            } else {
                _pdfExportStatus.value = "Failed to render PDF export document."
            }
        }
    }

    fun shareLastExportedPdf(context: android.content.Context) {
        val lastInfo = _lastExportedPdfInfo.value
        if (lastInfo != null && lastInfo.first.exists()) {
            com.example.util.PdfReportGenerator.sharePdfReportViaIntent(context, lastInfo.second, lastInfo.first)
        } else {
            exportFirestoreSensorHistoryToPdf(context)
        }
    }

    // --- NFC / RFID Background Service Operations ---
    fun startNfcBackgroundService() {
        val intent = android.content.Intent(getApplication(), com.example.nfc.NfcRfidBackgroundService::class.java).apply {
            action = com.example.nfc.NfcRfidBackgroundService.ACTION_START
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("EcoMindViewModel", "Foreground service launch note: ${e.message}")
        }
        nfcRfidScannerManager.startNfcService()
    }

    fun stopNfcBackgroundService() {
        val intent = android.content.Intent(getApplication(), com.example.nfc.NfcRfidBackgroundService::class.java).apply {
            action = com.example.nfc.NfcRfidBackgroundService.ACTION_STOP
        }
        try {
            getApplication<Application>().stopService(intent)
        } catch (e: Exception) {
            android.util.Log.e("EcoMindViewModel", "Stop service note: ${e.message}")
        }
        nfcRfidScannerManager.stopNfcService()
    }

    fun triggerPhysicalNfcScan(
        tagId: String = "NFC-TAG-1001",
        locationId: String = "LOC-HYDRO-01",
        locationName: String = "Zone A - Hydroponics Greenhouse",
        temp: Float = 24.2f,
        hum: Float = 52.5f,
        co2: Float = 420.0f
    ) {
        nfcRfidScannerManager.processAndLinkTagScan(tagId, locationId, locationName, temp, hum, co2)
        onRfidTagScanned(tagId)
    }

    // --- Firebase Cloud Messaging (FCM) Push Notifications ---
    fun checkAndTriggerCriticalSensorPushNotification(
        context: android.content.Context,
        tempC: Float,
        humPercent: Float,
        co2Ppm: Float,
        deviceName: String = "Environment Node"
    ) {
        val alerts = mutableListOf<String>()
        var primaryType = "ENVIRONMENTAL"

        if (co2Ppm >= 1000f) {
            alerts.add("CRITICAL CO2 Level: ${co2Ppm.toInt()} PPM (Threshold: 1000 PPM)")
            primaryType = "AIR_QUALITY_CO2"
        }
        if (tempC > 35f || tempC < 5f) {
            alerts.add("CRITICAL Temp: %.1f°C (Safe Range: 5°C - 35°C)".format(tempC))
            primaryType = "TEMPERATURE"
        }
        if (humPercent > 85f || humPercent < 20f) {
            alerts.add("CRITICAL Humidity: %.1f%% (Safe Range: 20%% - 85%%)".format(humPercent))
            primaryType = "HUMIDITY"
        }

        if (alerts.isNotEmpty()) {
            _criticalAlertCount.value += 1
            val alertSummary = alerts.joinToString(" | ")
            _lastPushNotificationAlert.value = "🚨 [${deviceName}] $alertSummary"

            com.example.fcm.EcoMindMessagingService.sendPushNotification(
                context = context,
                title = "🚨 Critical Environmental Alert ($deviceName)",
                body = alertSummary,
                sensorType = primaryType,
                alertLevel = "CRITICAL"
            )
        }
    }

    fun refreshFcmToken() {
        com.example.fcm.EcoMindMessagingService.fetchFcmToken(getApplication()) { token ->
            _fcmRegistrationToken.value = token ?: "Token unavailable (Check Google Play Services)"
        }
    }

    // --- Sensor Offset Calibration Methods ---
    fun updateGlobalCalibration(tempOffset: Float, humOffset: Float, co2Offset: Float) {
        _globalTempOffset.value = tempOffset
        _globalHumOffset.value = humOffset
        _globalCo2Offset.value = co2Offset

        prefs.edit()
            .putFloat("global_temp_offset", tempOffset)
            .putFloat("global_hum_offset", humOffset)
            .putFloat("global_co2_offset", co2Offset)
            .apply()
    }

    fun resetGlobalCalibration() {
        updateGlobalCalibration(0.0f, 0.0f, 0.0f)
    }

    fun updateDeviceCalibration(deviceId: String, deviceName: String, tempOffset: Float, humOffset: Float, co2Offset: Float) {
        val updatedMap = _deviceCalibrations.value.toMutableMap()
        updatedMap[deviceId] = SensorCalibrationConfig(
            deviceId = deviceId,
            deviceName = deviceName,
            tempOffsetC = tempOffset,
            humidityOffsetPercent = humOffset,
            co2OffsetPpm = co2Offset
        )
        _deviceCalibrations.value = updatedMap

        prefs.edit()
            .putFloat("${deviceId}_temp_offset", tempOffset)
            .putFloat("${deviceId}_hum_offset", humOffset)
            .putFloat("${deviceId}_co2_offset", co2Offset)
            .apply()
    }

    fun updateSafetyThresholds(co2Ppm: Float, tempC: Float, humPercent: Float) {
        _co2ThresholdPpm.value = co2Ppm
        _tempThresholdC.value = tempC
        _humThresholdPercent.value = humPercent

        prefs.edit()
            .putFloat("co2_threshold_ppm", co2Ppm)
            .putFloat("temp_threshold_c", tempC)
            .putFloat("hum_threshold_percent", humPercent)
            .apply()
    }
}

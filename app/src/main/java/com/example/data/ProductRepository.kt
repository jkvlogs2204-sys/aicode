package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProductRepository(
    private val dao: ProductDao,
    private val firestoreSyncHelper: FirestoreSyncHelper? = null
) {
    private val TAG = "ProductRepository"

    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val scanHistory: Flow<List<ScanHistoryEntity>> = dao.getScanHistory()

    fun getProductById(id: String): Flow<ProductEntity?> = dao.getProductById(id)

    suspend fun getProductByIdDirect(id: String): ProductEntity? = withContext(Dispatchers.IO) {
        val local = dao.getProductByIdDirect(id)
        if (local != null) return@withContext local

        // Try fetching directly from Firestore document
        try {
            val doc = FirebaseFirestore.getInstance().collection("products").document(id).get().await()
            if (doc.exists()) {
                val p = ProductEntity(
                    id = doc.getString("id") ?: id,
                    name = doc.getString("name") ?: "Eco Product",
                    category = doc.getString("category") ?: "General",
                    carbon = doc.getString("carbon") ?: "0.5 kg",
                    water = doc.getString("water") ?: "5.0 L",
                    ecoScore = doc.getLong("ecoScore")?.toInt() ?: 75,
                    recycling = doc.getString("recycling") ?: "Recyclable",
                    impact = doc.getString("impact") ?: "Firestore Production Product",
                    alternative = doc.getString("alternative") ?: "Sustainable Alternative",
                    isEcoFriendly = doc.getBoolean("isEcoFriendly") ?: true
                )
                dao.insertProduct(p)
                return@withContext p
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore direct fetch failed for id $id: ${e.message}")
        }
        
        // Search in initial fallback list
        val sampleMatch = SampleData.initialProducts.firstOrNull { it.id == id }
        if (sampleMatch != null) {
            dao.insertProduct(sampleMatch)
            return@withContext sampleMatch
        }
        return@withContext null
    }

    suspend fun fetchFromRemoteBackend(baseUrl: String, id: String): ProductEntity? = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.createBackendApi(baseUrl)
            val response = try {
                api.getProductByRfidUid(id)
            } catch (_: Exception) {
                api.getProductById(id)
            }
            val finalScore = response.eco_score ?: response.ecoScore
            val entity = ProductEntity(
                id = id,
                name = response.name,
                category = response.category ?: "General",
                carbon = response.carbon,
                water = response.water,
                ecoScore = finalScore,
                recycling = response.recycling,
                impact = response.impact ?: "Fetched from remote server.",
                alternative = response.alternative,
                isEcoFriendly = response.isEcoFriendly ?: (finalScore >= 60)
            )
            dao.insertProduct(entity)
            // Sync to Firestore
            firestoreSyncHelper?.uploadProductsToFirestore(listOf(entity))
            entity
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun addScanHistory(productId: String, productName: String, category: String, ecoScore: Int, source: String = "RFID Bluetooth") {
        withContext(Dispatchers.IO) {
            val entity = ScanHistoryEntity(
                productId = productId,
                productName = productName,
                category = category,
                ecoScore = ecoScore,
                source = source
            )
            dao.insertScanHistory(entity)

            // Push to Firestore scan_history collection
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docData = hashMapOf(
                    "productId" to productId,
                    "productName" to productName,
                    "category" to category,
                    "ecoScore" to ecoScore,
                    "source" to source,
                    "timestamp" to entity.timestamp
                )
                firestore.collection("scan_history")
                    .document("${entity.timestamp}_$productId")
                    .set(docData, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Sync scan history to Firestore failed: ${e.message}")
            }
        }
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> = dao.searchProducts(query)

    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> = dao.getProductsByCategory(category)

    suspend fun updateProduct(product: ProductEntity) {
        withContext(Dispatchers.IO) {
            dao.insertProduct(product)
            // Sync to Firestore
            firestoreSyncHelper?.uploadProductsToFirestore(listOf(product))
        }
    }

    suspend fun deleteProduct(id: String) {
        withContext(Dispatchers.IO) {
            dao.deleteProduct(id)
            try {
                FirebaseFirestore.getInstance().collection("products").document(id).delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Delete product from Firestore failed: ${e.message}")
            }
        }
    }

    suspend fun fetchProductsFromFirestore(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        firestoreSyncHelper?.fetchProductsFromFirestore() ?: Pair(false, "Firestore helper not initialized")
    }

    suspend fun syncProductsToFirestore(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val currentProducts = dao.getAllProductsDirect()
        firestoreSyncHelper?.uploadProductsToFirestore(currentProducts) ?: Pair(false, "Firestore helper not initialized")
    }

    suspend fun syncProductToBackend(baseUrl: String, product: ProductEntity): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.createBackendApi(baseUrl)
            val payload = ProductApiResponse(
                id = product.id,
                name = product.name,
                category = product.category,
                carbon = product.carbon,
                water = product.water,
                ecoScore = product.ecoScore,
                recycling = product.recycling,
                impact = product.impact,
                alternative = product.alternative,
                isEcoFriendly = product.isEcoFriendly
            )
            api.createOrUpdateProduct(payload)
            Pair(true, "Product '${product.name}' pushed to remote backend ($baseUrl)")
        } catch (e: Exception) {
            Log.w(TAG, "syncProductToBackend failed: ${e.message}")
            Pair(false, "Push failed: ${e.localizedMessage ?: "Could not connect to $baseUrl"}")
        }
    }

    suspend fun syncFullDatabaseToBackend(
        baseUrl: String,
        products: List<ProductEntity>,
        sensorReadings: List<SensorReadingEntity>
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.createBackendApi(baseUrl)
            val productPayloads = products.map { p ->
                ProductApiResponse(
                    id = p.id,
                    name = p.name,
                    category = p.category,
                    carbon = p.carbon,
                    water = p.water,
                    ecoScore = p.ecoScore,
                    recycling = p.recycling,
                    impact = p.impact,
                    alternative = p.alternative,
                    isEcoFriendly = p.isEcoFriendly
                )
            }
            val sensorPayloads = sensorReadings.map { s ->
                SensorReadingPayload(
                    deviceName = s.deviceName,
                    temperatureC = s.temperatureC,
                    humidityPercent = s.humidityPercent,
                    co2Ppm = s.co2Ppm,
                    timestamp = s.timestamp
                )
            }
            val payload = DatabaseSyncPayload(
                products = productPayloads,
                sensorReadings = sensorPayloads
            )
            val response = api.syncDatabasePayload(payload)
            val serverMsg = response["message"]?.toString() ?: "Synced successfully"
            Pair(true, "Success: $serverMsg (${products.size} products, ${sensorReadings.size} sensor logs) to $baseUrl")
        } catch (e: Exception) {
            Log.w(TAG, "syncFullDatabaseToBackend failed: ${e.message}")
            Pair(false, "Push to server failed: ${e.localizedMessage ?: "Cannot reach $baseUrl. Verify server is running"}")
        }
    }

    suspend fun testBackendConnection(baseUrl: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.createBackendApi(baseUrl)
            try {
                val health = api.checkHealth()
                Pair(true, "Backend Online: Healthy & Responsive ($baseUrl)")
            } catch (e: Exception) {
                val products = api.getAllProducts()
                Pair(true, "Backend Connected: Retrieved ${products.size} remote records from $baseUrl")
            }
        } catch (e: Exception) {
            Log.w(TAG, "testBackendConnection failed: ${e.message}")
            Pair(false, "Connection Failed: Cannot reach $baseUrl (${e.localizedMessage ?: "Offline"})")
        }
    }

    suspend fun fetchFullDatabaseFromBackend(baseUrl: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val api = NetworkClient.createBackendApi(baseUrl)
            val remoteProducts = api.getAllProducts()
            if (remoteProducts.isNotEmpty()) {
                remoteProducts.forEach { p ->
                    val entity = ProductEntity(
                        id = p.id ?: "10${(10..99).random()}",
                        name = p.name,
                        category = p.category ?: "General",
                        carbon = p.carbon,
                        water = p.water,
                        ecoScore = p.ecoScore,
                        recycling = p.recycling,
                        impact = p.impact ?: "Eco product synced from remote",
                        alternative = p.alternative,
                        isEcoFriendly = p.isEcoFriendly ?: true
                    )
                    dao.insertProduct(entity)
                }
                firestoreSyncHelper?.uploadProductsToFirestore(dao.getAllProductsDirect())
                Pair(true, "Successfully imported ${remoteProducts.size} products from REST API into Room DB")
            } else {
                Pair(false, "Remote database at $baseUrl returned 0 records")
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchFullDatabaseFromBackend failed: ${e.message}")
            Pair(false, "Fetch Failed: Could not sync with $baseUrl (${e.localizedMessage})")
        }
    }

    suspend fun resetDatabaseToDefaults() = withContext(Dispatchers.IO) {
        dao.clearAllProducts()
        dao.insertProducts(SampleData.initialProducts)
        firestoreSyncHelper?.uploadProductsToFirestore(SampleData.initialProducts)
    }

    suspend fun ensureSeeded() {
        withContext(Dispatchers.IO) {
            // First attempt to fetch real production data from Cloud Firestore
            if (firestoreSyncHelper != null) {
                val firestoreResult = firestoreSyncHelper.fetchProductsFromFirestore()
                Log.d(TAG, "Firestore product seed check: ${firestoreResult.second}")
            }

            // If local count is still 0 (e.g. offline or new Firestore instance), seed initial records and upload to Firestore
            val count = dao.getProductsCount()
            if (count == 0) {
                dao.insertProducts(SampleData.initialProducts)
                firestoreSyncHelper?.uploadProductsToFirestore(SampleData.initialProducts)
            }
        }
    }
}


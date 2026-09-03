package com.example.data

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SensorRepository(
    private val dao: SensorReadingDao,
    private val firestoreSyncHelper: FirestoreSyncHelper? = null
) {
    private val TAG = "SensorRepository"

    val recentReadings: Flow<List<SensorReadingEntity>> = dao.getRecentReadings(50)
    val allReadingsAsc: Flow<List<SensorReadingEntity>> = dao.getAllReadingsAsc()

    /**
     * Real-time Firestore snapshot listener flow.
     * Continuously observes the 'environmental_data' collection in Cloud Firestore in real time.
     * When any environmental sensor doc is added or updated, it pushes records directly into
     * the Room local database, triggering automatic UI recomposition on the Dashboard.
     */
    fun observeRealtimeFirestoreSensorData(): Flow<List<SensorReadingEntity>> = callbackFlow {
        val firestore = try {
            val isAppInitialized = try { FirebaseApp.getInstance() != null } catch (e: Throwable) { false }
            if (!isAppInitialized) {
                Log.w(TAG, "FirebaseApp is not initialized, real-time Firestore listener unavailable")
                null
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Firestore instance unavailable for real-time listener: ${e.message}")
            null
        }

        if (firestore == null) {
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("environmental_data")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Real-time Firestore listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val readings = snapshot.documents.mapNotNull { doc ->
                        val temp = doc.getDouble("temperatureC")?.toFloat()
                            ?: doc.getString("temperatureC")?.toFloatOrNull()
                        val hum = doc.getDouble("humidityPercent")?.toFloat()
                            ?: doc.getString("humidityPercent")?.toFloatOrNull() ?: 50.0f
                        val co2 = doc.getDouble("co2Ppm")?.toFloat()
                            ?: doc.getString("co2Ppm")?.toFloatOrNull() ?: 410.0f
                        val deviceName = doc.getString("deviceName") ?: "Cloud Sensor Node"
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                        if (temp != null) {
                            SensorReadingEntity(
                                deviceName = deviceName,
                                temperatureC = temp,
                                humidityPercent = hum,
                                co2Ppm = co2,
                                timestamp = timestamp
                            )
                        } else null
                    }

                    if (readings.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            dao.insertReadings(readings)
                        }
                    }

                    trySend(readings)
                }
            }

        awaitClose {
            Log.d(TAG, "Removing real-time Firestore sensor listener")
            listenerRegistration.remove()
        }
    }

    suspend fun insertReading(reading: SensorReadingEntity) {
        withContext(Dispatchers.IO) {
            dao.insertReading(reading)
            firestoreSyncHelper?.uploadSensorReading(reading)
        }
    }

    suspend fun insertReadings(readings: List<SensorReadingEntity>) {
        withContext(Dispatchers.IO) {
            dao.insertReadings(readings)
            firestoreSyncHelper?.syncAllSensorReadingsToFirestore(readings)
        }
    }

    suspend fun fetchFromFirestore(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        firestoreSyncHelper?.fetchSensorReadingsFromFirestore() ?: Pair(false, "Firestore helper not initialized")
    }

    suspend fun syncToFirestore(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val readings = dao.getRecentReadingsDirect(50)
        firestoreSyncHelper?.syncAllSensorReadingsToFirestore(readings) ?: Pair(false, "Firestore helper not initialized")
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            dao.clearAllReadings()
        }
    }
}


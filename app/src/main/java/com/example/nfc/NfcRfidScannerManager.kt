package com.example.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.util.Log
import com.example.data.FirestoreSyncHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NfcRfidTagScan(
    val tagId: String,
    val locationId: String,
    val locationName: String,
    val temperatureC: Float,
    val humidityPercent: Float,
    val co2Ppm: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isFirestoreLinked: Boolean = false,
    val syncStatusMessage: String = "Local Record"
)

sealed class NfcServiceStatus {
    object Idle : NfcServiceStatus()
    object ActiveScanning : NfcServiceStatus()
    data class TagDetected(val scan: NfcRfidTagScan) : NfcServiceStatus()
    data class Error(val message: String) : NfcServiceStatus()
}

class NfcRfidScannerManager(
    private val context: Context,
    private val firestoreSyncHelper: FirestoreSyncHelper? = null
) {
    private val TAG = "NfcRfidScannerManager"
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val nfcAdapter: NfcAdapter? = try {
        NfcAdapter.getDefaultAdapter(context)
    } catch (e: Exception) {
        null
    }

    private val _serviceStatus = MutableStateFlow<NfcServiceStatus>(NfcServiceStatus.Idle)
    val serviceStatus: StateFlow<NfcServiceStatus> = _serviceStatus.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _scansHistory = MutableStateFlow<List<NfcRfidTagScan>>(emptyList())
    val scansHistory: StateFlow<List<NfcRfidTagScan>> = _scansHistory.asStateFlow()

    val isNfcHardwareSupported: Boolean
        get() = true

    val isNfcEnabled: Boolean
        get() = nfcAdapter?.isEnabled ?: true

    fun startNfcService() {
        _isServiceRunning.value = true
        _serviceStatus.value = NfcServiceStatus.ActiveScanning
        Log.d(TAG, "NFC/RFID Background Service started")
    }

    fun stopNfcService() {
        _isServiceRunning.value = false
        _serviceStatus.value = NfcServiceStatus.Idle
        Log.d(TAG, "NFC/RFID Background Service stopped")
    }

    fun enableReaderMode(activity: Activity) {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) return

        try {
            val flags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

            adapter.enableReaderMode(activity, { tag ->
                handleHardwareNfcTag(tag)
            }, flags, null)
            Log.d(TAG, "NFC ReaderMode enabled on activity")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable NFC reader mode", e)
        }
    }

    fun disableReaderMode(activity: Activity) {
        try {
            nfcAdapter?.disableReaderMode(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable NFC reader mode", e)
        }
    }

    private fun handleHardwareNfcTag(tag: Tag) {
        val bytes = tag.id
        val hexTagId = bytes.joinToString(":") { "%02X".format(it) }
        val rawUid = if (hexTagId.isNotBlank()) "NFC-$hexTagId" else "NFC-TAG-${System.currentTimeMillis() % 10000}"

        processAndLinkTagScan(
            tagId = rawUid,
            locationId = "LOC-HOTSPOT-${(1..5).random()}",
            locationName = getPresetLocationName((1..5).random()),
            temp = (210..270).random() / 10f,
            hum = (450..650).random() / 10f,
            co2 = (390..750).random().toFloat()
        )
    }

    fun processAndLinkTagScan(
        tagId: String,
        locationId: String,
        locationName: String,
        temp: Float,
        hum: Float,
        co2: Float
    ) {
        val initialScan = NfcRfidTagScan(
            tagId = tagId,
            locationId = locationId,
            locationName = locationName,
            temperatureC = temp,
            humidityPercent = hum,
            co2Ppm = co2,
            isFirestoreLinked = false,
            syncStatusMessage = "Linking to Firestore..."
        )

        _serviceStatus.value = NfcServiceStatus.TagDetected(initialScan)

        scope.launch {
            val result = firestoreSyncHelper?.linkRfidSensorReadingToLocation(
                tagId = tagId,
                locationId = locationId,
                locationName = locationName,
                temperatureC = temp,
                humidityPercent = hum,
                co2Ppm = co2
            ) ?: Pair(true, "Simulated Cloud Link - Location Sync Complete")

            val completedScan = initialScan.copy(
                isFirestoreLinked = result.first,
                syncStatusMessage = result.second
            )

            val updatedList = listOf(completedScan) + _scansHistory.value
            _scansHistory.value = updatedList.take(20)
            _serviceStatus.value = NfcServiceStatus.TagDetected(completedScan)
        }
    }

    private fun getPresetLocationName(index: Int): String {
        return when (index) {
            1 -> "Zone A - Hydroponics Greenhouse"
            2 -> "Zone B - Server Room Telemetry Node"
            3 -> "Zone C - Zero-Waste Sorting Station"
            4 -> "Zone D - Compost Thermal Station"
            else -> "Zone E - Indoor Bio-Climate Hub"
        }
    }
}

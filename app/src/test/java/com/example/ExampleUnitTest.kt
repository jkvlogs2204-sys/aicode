package com.example

import com.example.data.DatabaseSyncSummary
import org.junit.Assert.*
import org.junit.Test

/**
 * Local unit test verifying cloud database persistence models and sync states.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDatabaseSyncSummary_successComputation() {
    val summary = DatabaseSyncSummary(
      productsCount = 12,
      sensorReadingsCount = 45,
      rfidMappingsCount = 8,
      scanHistoryCount = 30,
      lastBackupTimeMs = System.currentTimeMillis(),
      success = true,
      message = "All records backed up"
    )

    assertTrue(summary.success)
    assertEquals(12, summary.productsCount)
    assertEquals(45, summary.sensorReadingsCount)
    assertEquals(8, summary.rfidMappingsCount)
    assertEquals(30, summary.scanHistoryCount)
    assertEquals("All records backed up", summary.message)
  }

  @Test
  fun testDatabaseSyncSummary_failureHandling() {
    val errorSummary = DatabaseSyncSummary(
      message = "Network timeout communicating with Cloud Firestore",
      success = false
    )

    assertFalse(errorSummary.success)
    assertEquals(0, errorSummary.productsCount)
    assertEquals("Network timeout communicating with Cloud Firestore", errorSummary.message)
  }

  @Test
  fun testDiscoveredBleDevice_hc05Detection() {
    val hc05Device = com.example.ui.DiscoveredBleDevice(
      name = "HC-05",
      address = "98:D3:31:FC:21:44",
      rssi = -62,
      category = com.example.ui.BleDeviceCategory.HC05_MODULE,
      isHc05 = true
    )

    assertTrue(hc05Device.isHc05)
    assertEquals(com.example.ui.BleDeviceCategory.HC05_MODULE, hc05Device.category)
    assertEquals("98:D3:31:FC:21:44", hc05Device.address)
    assertEquals(-62, hc05Device.rssi)
  }

  @Test
  fun testRealTimeIotTelemetry_dataParsing() {
    val telemetry = com.example.ui.RealTimeIotTelemetry(
      temperatureC = 25.4f,
      humidityPercent = 58.2f,
      gasPpm = 135.0f,
      soilMoisturePercent = 62.0f,
      waterLevelPercent = 45.0f,
      lightLux = 420.0f,
      scannedRfidTag = "PRODUCT:E28011606000020473919424",
      rawPayload = "TEMP:25.4,HUM:58.2,GAS:135.0,SOIL:62.0,LIGHT:420,RFID:E28011606000020473919424",
      packetCount = 10
    )

    assertEquals(25.4f, telemetry.temperatureC, 0.01f)
    assertEquals(58.2f, telemetry.humidityPercent, 0.01f)
    assertEquals(135.0f, telemetry.gasPpm, 0.01f)
    assertEquals(62.0f, telemetry.soilMoisturePercent, 0.01f)
    assertEquals(420.0f, telemetry.lightLux, 0.01f)
    assertEquals("PRODUCT:E28011606000020473919424", telemetry.scannedRfidTag)
    assertEquals(10L, telemetry.packetCount)
  }

  @Test
  fun testBleDeviceConnectionStatus_states() {
    val disconnected = com.example.ui.BleDeviceConnectionStatus.Disconnected
    val connecting = com.example.ui.BleDeviceConnectionStatus.Connecting("HC-05 Node", "98:D3:31:FC:21:44")
    val connectedGatt = com.example.ui.BleDeviceConnectionStatus.Connected("HC-05 Node", "98:D3:31:FC:21:44", "BLE GATT (Low Energy)")
    val connectedSpp = com.example.ui.BleDeviceConnectionStatus.Connected("HC-05 Node", "98:D3:31:FC:21:44", "Bluetooth Classic SPP (HC-05)")

    assertTrue(disconnected is com.example.ui.BleDeviceConnectionStatus.Disconnected)
    assertEquals("HC-05 Node", connecting.deviceName)
    assertEquals("BLE GATT (Low Energy)", connectedGatt.connectionMode)
    assertEquals("Bluetooth Classic SPP (HC-05)", connectedSpp.connectionMode)
  }

  @Test
  fun testGeminiEcoDecision_scoring() {
    val (colorGreen, gradeA, _) = com.example.ai.GeminiEcoAssistant.computeEcoDecision(92)
    assertEquals("GREEN", colorGreen)
    assertEquals("A+", gradeA)

    val (colorYellow, gradeB, _) = com.example.ai.GeminiEcoAssistant.computeEcoDecision(65)
    assertEquals("YELLOW", colorYellow)
    assertEquals("B", gradeB)

    val (colorRed, gradeE, _) = com.example.ai.GeminiEcoAssistant.computeEcoDecision(15)
    assertEquals("RED", colorRed)
    assertEquals("E", gradeE)
  }

  @Test
  fun testGeminiRequest_serializationFormat() {
    val req = com.example.data.GeminiRequest(
      contents = listOf(
        com.example.data.GeminiContent(
          parts = listOf(com.example.data.GeminiPart(text = "Hello Eco Mind")),
          role = "user"
        )
      ),
      systemInstruction = com.example.data.GeminiContent(
        parts = listOf(com.example.data.GeminiPart(text = "You are an eco AI")),
        role = "system"
      )
    )

    assertEquals("user", req.contents.first().role)
    assertEquals("Hello Eco Mind", req.contents.first().parts.first().text)
    assertEquals("You are an eco AI", req.systemInstruction?.parts?.first()?.text)
  }
}

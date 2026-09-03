/*
 * Eco Mind AI - Arduino Uno RFID & HC-05 Bluetooth Transmitter
 * 
 * Communication Flow (Unidirectional Hardware Telemetry):
 * [RFID Tag] -> [MFRC522] -> [Arduino Uno] -> [HC-05 Bluetooth] -> [Mobile App]
 * 
 * Hardware Required:
 * - Arduino Uno
 * - RC522 RFID Reader (SPI)
 * - HC-05 Bluetooth Module (SoftwareSerial TX/RX)
 * 
 * Pin Connections:
 * 
 * RC522 RFID SPI:
 * - SDA (SS)  -> Pin 10
 * - SCK       -> Pin 13
 * - MOSI      -> Pin 11
 * - MISO      -> Pin 12
 * - IRQ       -> Not Connected
 * - GND       -> GND
 * - RST       -> Pin 9
 * - 3.3V      -> 3.3V (Do NOT connect to 5V!)
 * 
 * HC-05 Bluetooth Module:
 * - VCC       -> 5V
 * - GND       -> GND
 * - TX        -> Pin 2 (Arduino RX)
 * - RX        -> Pin 3 (Arduino TX through voltage divider 1k/2k ohm)
 * 
 * Optional Auxiliary Pins (Reserved for future hardware expansion; NOT controlled by Eco Score):
 * - RGB / Status LED -> Pin 5 / Pin 4
 * - Servo            -> Pin 6
 */

#include <SPI.h>
#include <MFRC522.h>
#include <SoftwareSerial.h>

#define SS_PIN 10
#define RST_PIN 9

MFRC522 rfid(SS_PIN, RST_PIN);
SoftwareSerial bluetooth(2, 3); // RX = Pin 2, TX = Pin 3

void setup() {
  Serial.begin(9600);
  bluetooth.begin(9600);
  SPI.begin();
  rfid.PCD_Init();

  Serial.println(F("Eco Mind AI RFID Scanner Ready. Transmitting UIDs via HC-05."));
  bluetooth.println(F("SYSTEM_READY"));
}

void loop() {
  // Check for RFID Tag Scans
  if (!rfid.PICC_IsNewCardPresent()) return;
  if (!rfid.PICC_ReadCardSerial()) return;

  // Read and normalize UID as uppercase hexadecimal string (e.g. A1B2C3D4)
  String uidHex = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (rfid.uid.uidByte[i] < 0x10) {
      uidHex += "0";
    }
    uidHex += String(rfid.uid.uidByte[i], HEX);
  }
  uidHex.toUpperCase();

  // Standardized UID transmission payload: PRODUCT:<UID>
  String productMsg = "PRODUCT:" + uidHex;

  Serial.print(F("Scanned Tag UID: "));
  Serial.print(uidHex);
  Serial.print(F(" -> Sending to App: "));
  Serial.println(productMsg);

  // Send UID through HC-05 to Mobile App (Unidirectional: Arduino -> Mobile App)
  bluetooth.println(productMsg);

  // Halt PICC to avoid duplicate immediate reads
  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
  delay(1200); // Debounce scan cooldown
}


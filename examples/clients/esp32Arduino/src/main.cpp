/*
 * (C) Copyright 2019 Jens Heuschkel.
 */

#include <Arduino.h>
#include <BLEDevice.h>

// Conversion factor for micro seconds to seconds 
#define uS_TO_S_FACTOR 1000000  

// The remote service of GlucoProxBLE
static BLEUUID advServiceUUID("00001801-0000-1000-8000-00805f9b34fb");
// the broadcast ID we want to subscribe to
static uint16_t searchedID = 0x03; // set 0 to scan all glucoprox devices.

BLEAdvertisedDevice* advDevice;

// buffers for processing glucose data
SemaphoreHandle_t xBinarySemaphore;
uint16_t* glucoseDataBuffer= new uint16_t[20];
int16_t glucoseDataBufferSize = 0;
uint16_t deviceId = 0;

uint16_t decompressBG(byte compressedBG){
  if (compressedBG == 0) 
    return (uint16_t) 0;
  else if (compressedBG == 1)
    return 25;
  else if (compressedBG == 2)
    return 420;
  else if(compressedBG < 152){
    return compressedBG + 27;
  }
  else if(compressedBG < 207){
    return (compressedBG - 153) * 2 + 180;
  }
  else {
    return (compressedBG - 208) * 3 + 290;
  }
  return (uint16_t) -1;
}

/**
 * Scan for BLE servers and find the first one that advertises the service we are looking for.
 */
class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
 /**
   * Called for each advertising BLE server.
   */
  void onResult(BLEAdvertisedDevice advertisedDevice) {
    Serial.print("BLE Advertised Device found: ");
    Serial.println(advertisedDevice.toString().c_str());
    
    // We have found a device, let us now see if it contains the service we are looking for.
    if (advertisedDevice.isAdvertisingService(advServiceUUID)) {
      advertisedDevice.getScan()->stop();
      if( xSemaphoreTake( xBinarySemaphore, ( TickType_t ) 5 ) == pdTRUE ) {
        // found a device with the correct service -> stop scanning and process the advertisement data.
        Serial.println("--> Found a GlucoProxBLE device!"); 

        const char* advData = advertisedDevice.getServiceData().c_str();
        glucoseDataBufferSize = advertisedDevice.getServiceData().size() -2;

        if (glucoseDataBufferSize < 3){
          Serial.println("data Packet too small");
          xSemaphoreGive(xBinarySemaphore);
          return;
        }
        deviceId = advData[0] | advData[1] << 8 ;
        
        Serial.print("ID: 0x");
        Serial.print(deviceId, HEX);
        Serial.print(" (Raw: 0x ");
        Serial.print(advData[0], HEX);
        Serial.print(" ");
        Serial.print(advData[1], HEX);
        Serial.println(")");
        if (searchedID != 0 && deviceId != searchedID){
          Serial.println(" NOT SEARCHED ID!");
          xSemaphoreGive(xBinarySemaphore);
          return;
        }

        Serial.print("Raw data: 0x ");
        for (uint8_t i = 0; i < glucoseDataBufferSize; i++){
          byte b = (byte) advData[i+3];
          glucoseDataBuffer[i] = decompressBG(b);
          Serial.print(b, HEX);
          Serial.print(" ");
        }
        Serial.println("");
        
        Serial.print("Time Offset (min): ");
        Serial.println(advData[2], DEC);

        Serial.print("Values (mg/dl): ");
        for (uint8_t i = 0; i < glucoseDataBufferSize; i++){
          uint16_t value = glucoseDataBuffer[i];
          Serial.print(value);
          Serial.print(", ");
        }
        Serial.println("");
        Serial.println("##################################");
        Serial.println("");

        xSemaphoreGive(xBinarySemaphore);
      } else {
        Serial.println("Semaphore blocked!--------------");
      }
    } 
  } 
}; // MyAdvertisedDeviceCallbacks

void setup() {
  Serial.begin(115200);
  Serial.println("Starting Arduino BLE Client application...");
  xBinarySemaphore = xSemaphoreCreateBinary();
  xSemaphoreGive(xBinarySemaphore);
  BLEDevice::init("");
}

void loop() {
  Serial.println("start scanning"); 
  BLEScan* pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setActiveScan(false);
  pBLEScan->start(330); // this will block for 5,5 min (or until scan stopped)
  pBLEScan->stop();
  delay(5000); // this is important to give the callback function a chance to finish until a new scan starts...
}
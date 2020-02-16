# GlucoProxBLE
GlucoProxBLE aims to provide BG readings to other devices without requiring an Internet connection.
Therefore it reads broadcasts from CGM applications (e.g. xDrip) and sends the readings as BLE advertisements.
This should be a very (energy) efficient way to provide CGM readings to one or more receiving devices for further use (e.g. therapy assistance).

**NOTE**: Currently only mg/dl is supported. Check back later for mmol/l support.

## Warning !

This repository contains **very preliminary** code, intended for collaboration among developers.
It **is not ready** for end users and may be subject to rebasing without notice.

## Features
* Receiving broadcasts from:
  * xDrip+
  
* Sending BLE advertisements when received a new value
* AES encryption of the values
* Minimal user interface including a graph view to illustrate the data

## Interface
![Interface Overview](doc/interface.png)

## Receiving Data
A receiver has to scan for BLE advertisements to receive the packages.
As Android randomly changes its MAC address, a device ID is set via the user interface to identify the sender.

To receive the data on Debian-Linux based systems, install `bluez` and `bluez-hcidump` and run the following code:

```
sudo hcitool lescan --duplicates &  
sudo hcidump --raw
```

### Understanding BLE advertisement packet format
`sudo hcidump --raw` will produce output similar to this:

```
46:23:AB:20:44:5C (unknown)
> 04 3E 27 02 01 02 01 5C 44 20 AB 23 46 1B 03 03 01 18 16 16
  01 18 03 00 00 86 85 86 8A 8A 8D 91 93 94 94 92 00 00 00 00
  00 B1
``` 

* `0x 04` Preamble  
* `0x 3E 20 02 01` Access Address (maybe keep in mind for later use)
* `0x 02` Advertising package type: ADV_NONCONN_IND -> non-connectable undirected advertising event
* `0x 01` -> 0b0001 -> 00 Reserved -> 0 RxAdd 1 TxAdd (no idea ...)
* `0x 5C 44 20 AB 23 46` MAC Addresse (wich is changed regularly on Android)
* `0x 1B` BIT[8:13]：advertising data length （Maximum 37 bytes) BIT[14:15]：Reserved -> 0b 00**01 1011** -> 27
* `0x 03` Size: 3
* `0x 03` Type: Complete List of 16-bit Service Class UUIDs
* `0x 01 18` Service-UUID: 0x1801 -> Generic Attribute
* `0x 16` Size: 22
* `0x 16` Type: Service Data
* `0x 01 18` Service-UUID: 0x1801 -> Generic Attribute
* `0x 03 00` 10 bit Broadcast ID (set by user) -> 0x0003 -> 3  
* `0x 00` Time offset (or age) in minutes of the first BG reading.
* `0x 86 85 86 8A 8A 8D 91 93 94 94 92 00 00 00 00 00` (Size - Type - ServiceUUID - BroadcastID - TimeOffset = in this case 16) BG readings in 5min rythm (starting with the newest). Missing readings are filled with 0x00.
* `0x B1` CRC? (should be 3 bytes...)

I found [this](https://www.silabs.com/community/wireless/bluetooth/knowledge-base.entry.html/2017/02/10/bluetooth_advertisin-hGsf) article very helpful to understand the packet structure.


**NOTE**: If you have the missing information, consider opening an issue or sending a pull request :)

### Value Compression
Since advertisements can only carry very limited data, I wanted to fit one BG value into one byte. 
Therefore, I designed a very simple lossy compression method:
It is based on the assumption, that we don't need a exact value in high blood sugar range. 
Thus, the compression algorithm uses a higher step size than 1 mg/dl for the higher blood sugar range.
One byte is coded as following:

* `0x00` error or no value
* `0x01` LOW (<30 mg/dl)
* `0x02` HIGH (>428 mg/dl)
* `0x03 - 0x98` normal span for 30-180 mg/dl
* `0x99 - 0xCF` low compressed span for 182-290 mg/dl
* `0xD0 - 0xFE` high compressed span for 293-428 mg/dl

![Compression Illustration](doc/compression.png)

### Encryption
Currently I have no idea how to decrypt the messages.
Therefore, I recommend setting an empty password to disable the feature, or take a look at the encryption code in the class `.ble.AesEncryptionHelper` and update this section :)

## Clients
I've implemented a very simple client for receiving unencrypted bg readings with an ESP32.
It's implemented with Platform IO and the Arduino framework.
See examples/clients/esp32Arduino

## TODO
* Update documentation (especially missing parts of BLE package format)
* Verify that "Foreground Services" are good enough to run in background reliably
* Understand how to decrypt AES encrypted messages
* Provide a receiver app example for RPi & ESP32
* Since interpolation is not working well, search for bugs...
* testing, testing, testing ...

[//]: # (Note to future me: Look at this --> https://ukbaz.github.io/howto/beacon_scan_cmd_line.html)
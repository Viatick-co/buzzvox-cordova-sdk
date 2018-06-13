//
//  SmartLockSDK.swift
//  ViaUnlock
//
//  Created by Bie Yaqing on 14/2/17.
//  Copyright © 2017 Bie Yaqing. All rights reserved.
//

import Foundation
import CoreBluetooth
import CryptoSwift

struct LockBLEStruct {
    static let pri_ser_uuid = "FFE0"
    static let pri_cha_uuid = "FFF1"
}

protocol LockDeviceDelegate {
    func lockDevice(lockDevice: LockDevice, didConnected state: Bool)
    func lockDevice(lockDevice: LockDevice, didOpen state: Bool)
    func lockDevice(lockDevice: LockDevice, isTheMac state: Bool)
}

class LockDevice: NSObject {
    var peripheral: CBPeripheral? = nil
    var characteristic1: CBCharacteristic? = nil
    
    var id: NSNumber = 0
    var name: String = ""
    var password: String = ""
    var newPassword: String = ""
    var macAddress: String = ""
    var identifier: String = ""
    var state: NSNumber = 0
    var vibration: Bool = false
    var verified: Bool = false
    var connected: Bool = false
    var battery: NSNumber = 0
    
    var validateMac: String? = nil
    
    var WRITE_STAGE: String = ""
    
    var delegate: LockDeviceDelegate?
    
    init(peripheral: CBPeripheral, identifier: String, name: String, state: NSNumber) {
        self.peripheral = peripheral
        self.identifier = identifier
        self.name = name
        self.state = state
    }
    
    func sendCommand(data: NSData) {
        if (self.peripheral != nil) && self.peripheral?.state == CBPeripheralState.connected {
            if (self.characteristic1 != nil) {
                self.peripheral!.writeValue(data as Data, for: self.characteristic1!, type: .withResponse)
            }
        }
    }
    
    // unlock
    func unlock() {
        unlockRequest()
    }
    
    func unlockRequest() {
        let mdata: NSMutableData = NSMutableData()
        mdata.append([0xa1], length: 1)
        let pwdData: Data = self.password.data(using: .utf8)!
        mdata.append(pwdData)
        mdata.append([0x01], length: 1)
        WRITE_STAGE = "unlockRequest"
        self.sendCommand(data: mdata)
    }
    
    func unlockResponse() {
        let rtnData: Data = (self.characteristic1?.value)!
        let commandByte: [UInt8] = [UInt8](rtnData)
        if (rtnData.count == 3 && commandByte[0] == 0xa2 && commandByte[1] == 0x01 && commandByte[2] == 0x00) {
            self.delegate?.lockDevice(lockDevice: self, didOpen: true)
        } else {
            self.connected = false
            self.delegate?.lockDevice(lockDevice: self, didOpen: false)
        }
        // self.delegate?.lockDevice(lockDevice: self, didConnected: false)
    }
    
    // vibrate
    func vibrate() {
        vibrateRequest()
    }
    
    func vibrate(on: Bool) {
        let mdata: NSMutableData = NSMutableData()
        mdata.append([0xa1], length: 1)
        let pwdData: Data = self.password.data(using: .utf8)!
        mdata.append(pwdData)
        mdata.append([0x03], length: 1)
        if(on) {
            mdata.append([0x01], length: 1)
            self.vibration = false
        } else {
            mdata.append([0x00], length: 1)
            self.vibration = true
        }
        WRITE_STAGE = "vibrateRequest"
        self.sendCommand(data: mdata)
    }
    
    func vibrateRequest() {
        let mdata: NSMutableData = NSMutableData()
        mdata.append([0xa1], length: 1)
        let pwdData: Data = self.password.data(using: .utf8)!
        mdata.append(pwdData)
        mdata.append([0x03], length: 1)
        if(!self.vibration) {
            mdata.append([0x01], length: 1)
        } else {
            mdata.append([0x00], length: 1)
        }
        WRITE_STAGE = "vibrateRequest"
        self.sendCommand(data: mdata)
    }
    
    func vibrateResponse() {
        let rtnData: Data = (self.characteristic1?.value)!
        let commandByte: [UInt8] = [UInt8](rtnData)
        if (rtnData.count == 3 && commandByte[0] == 0xa2 && commandByte[1] == 0x03 && commandByte[2] == 0x00) {
            self.vibration = self.vibration == false
            print("reached!!")
        } else {
            self.connected = false
        }
    }
    
    // change password
    func changePassword() {
        if(self.newPassword.characters.count == 6) {
            changePasswordRequest()
        } else {
            print("MSG", "Password has to be 6...")
        }
    }
    
    func changePasswordRequest() {
        let mdata: NSMutableData = NSMutableData()
        mdata.append([0xa1], length: 1)
        let pwdData: Data = self.password.data(using: .utf8)!
        mdata.append(pwdData)
        mdata.append([0x07], length: 1)
        let newPwdData: Data = self.newPassword.data(using: .utf8)!
        mdata.append(newPwdData)
        WRITE_STAGE = "changePasswordRequest"
        self.sendCommand(data: mdata)
    }
    
    func changePasswordResponse() {
        let rtnData: Data = (self.characteristic1?.value)!
        let commandByte: [UInt8] = [UInt8](rtnData)
        if (rtnData.count == 3 && commandByte[0] == 0xa2 && commandByte[1] == 0x07 && commandByte[2] == 0x00) {
            self.password = self.newPassword
            print("MSG", "Password changed...")
        } else {
            self.connected = false
        }
    }
    
    // reset password
    func resetPassword() {
        resetPasswordRequest()
    }
    
    func resetPasswordRequest() {
        let mdata: NSMutableData = NSMutableData()
        mdata.append([0xa1, 0x31, 0x36, 0x38, 0x31, 0x36, 0x38, 0x08, 0x31, 0x36, 0x38, 0x31, 0x36, 0x38], length: 14)
        WRITE_STAGE = "resetPasswordRequest"
        self.sendCommand(data: mdata)
    }
    
    func resetPasswordResponse() {
        print("resetted...")
    }
    
    // first stage verification
    var CRC16DIC: Dictionary<String, NSMutableData>? = nil
    func verifyRequest() {
        // (0xa1) + 6 digits 741689 + (0x05) + 11 random digits
        let mdata: NSMutableData = NSMutableData()
        mdata.append([0xa1], length: 1)
        let pwdData: Data = "741689".data(using: .utf8)!
        mdata.append(pwdData)
        mdata.append([0x05], length: 1)
        CRC16DIC = SmartLockUtil.get11Digits()
        let ori11MData: NSMutableData = CRC16DIC!["oriMData"]!
        // let ency11MData: NSMutableData = CRC16DIC!["encyMData"]!
        mdata.append(ori11MData as Data)
        WRITE_STAGE = "verifyRequest"
        self.sendCommand(data: mdata)
    }
    
    func verifyResponse() {
        let rtnData: Data = (self.characteristic1?.value)!
        let commandByte: [UInt8] = [UInt8](rtnData)
        if (rtnData.count == 19 && commandByte[0] == 0xa2 && commandByte[1] == 0x05 && commandByte[2] == 0x00) {
            let macAddressRange: Range = 3..<9
            let macAddressData: Data = rtnData.subdata(in: macAddressRange)
            self.macAddress = (macAddressData as NSData).description.trimmingCharacters(in: CharacterSet.init(charactersIn: "<>")).replacingOccurrences(of: " ", with: "")
            if self.validateMac == nil {
                let subDataRange: Range = 9..<19
                let subData: Data = rtnData.subdata(in: subDataRange)
                if (subData == CRC16DIC!["encyMData"]! as Data) {
                    doubleVerifyRequest(rtnData: rtnData)
                }
            } else {
                if self.validateMac == self.macAddress {
                    self.delegate?.lockDevice(lockDevice: self, isTheMac: true)
                    let subDataRange: Range = 9..<19
                    let subData: Data = rtnData.subdata(in: subDataRange)
                    if (subData == CRC16DIC!["encyMData"]! as Data) {
                        doubleVerifyRequest(rtnData: rtnData)
                    }
                } else {
                    self.delegate?.lockDevice(lockDevice: self, isTheMac: false)
                }
            }
        }
    }
    
    // second stage verification
    func doubleVerifyRequest(rtnData: Data) {
        // (0xa1) + 6 digits 741689 + (0x09) + 12 random digits
        let mdata: NSMutableData = NSMutableData()
        mdata.append([0xa1], length: 1)
        let pwdData: Data = "741689".data(using: .utf8)!
        mdata.append(pwdData)
        mdata.append([0x09], length: 1)
        let dic:Dictionary<String, NSMutableData> = SmartLockUtil.get12Digits(data: rtnData)
        // let ori11MData: NSMutableData = dic["oriMData"]!
        let ency12MData: NSMutableData = dic["encyMData"]!
        mdata.append(ency12MData as Data)
        WRITE_STAGE = "doubleVerifyRequest"
        self.sendCommand(data: mdata)
    }
    
    func doubleVerifyResponse() {
        let rtnData: Data = (self.characteristic1?.value)!
        let commandByte: [UInt8] = [UInt8](rtnData)
        if (rtnData.count == 3 && commandByte[0] == 0xa2 && commandByte[1] == 0x09 && commandByte[2] == 0x00) {
            self.connected = true
            self.delegate?.lockDevice(lockDevice: self, didConnected: true)
        } else {
            self.connected = false
            self.delegate?.lockDevice(lockDevice: self, didConnected: false)
        }
    }
    
    func connect() {
        self.validateMac = nil
        self.peripheral?.delegate = self
        self.peripheral?.discoverServices([CBUUID(string: LockBLEStruct.pri_ser_uuid)])
    }
    
    func connect(mac: String) {
        self.validateMac = mac
        self.peripheral?.delegate = self
        self.peripheral?.discoverServices([CBUUID(string: LockBLEStruct.pri_ser_uuid)])
    }
    
    func discoverCharacteristic() {
        self.peripheral?.discoverCharacteristics([CBUUID(string: LockBLEStruct.pri_cha_uuid)], for: (peripheral?.services?[0])!)
    }
}

extension LockDevice: CBPeripheralDelegate {
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        self.discoverCharacteristic()
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        self.characteristic1 = service.characteristics?[0]
        self.verifyRequest()
    }
    
    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        peripheral.readValue(for: characteristic)
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        switch WRITE_STAGE {
            case "verifyRequest":
                self.verifyResponse()
            break
            case "doubleVerifyRequest":
                self.doubleVerifyResponse()
            break
            case "unlockRequest":
                self.unlockResponse()
            break
            case "vibrateRequest":
                self.vibrateResponse()
            break
            case "changePasswordRequest":
                self.changePasswordResponse()
            break
            case "resetPasswordRequest":
                self.resetPasswordResponse()
            break
            default:
            
            break
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverDescriptorsFor characteristic: CBCharacteristic, error: Error?) {
        
    }
}

protocol SmartLockControllerDelegate: class {
    func bluetooth(isBluetoothAvailable state: Bool)
    func bluetooth(isInitialised state: Bool)
    func device(didDiscoverDevice lockDevice: LockDevice)
    func device(didConnectDevice lockDevice: LockDevice)
    func device(didOpen lockDevice: LockDevice)
    func device(didNotOpen lockDevice: LockDevice)
    func devices(didLockDeviceArrayUpdated lockDevices:[LockDevice])
    func error(error message: String)
}

class SmartLockController: NSObject {
    static let sharedInstance = SmartLockController()
    
    var cbcManager: CBCentralManager!
    var lockDevices: [LockDevice] = []
    
    var globelMac: String? = nil
    
    weak var delegate: SmartLockControllerDelegate?
    
    var isDisconnect: Bool = false
    
    func initController() {
        globelMac = nil
        if cbcManager == nil {
            cbcManager = CBCentralManager(delegate: self, queue: nil, options: nil)
        } else {
            delegate?.bluetooth(isInitialised: true)
        }
    }
    
    func initController(mac: String) {
        globelMac = mac
        if cbcManager == nil {
            cbcManager = CBCentralManager(delegate: self, queue: nil, options: nil)
        } else {
            delegate?.bluetooth(isInitialised: true)
        }
    }
    
    func startScan() {
        cbcManager.scanForPeripherals(withServices: [CBUUID(string: LockBLEStruct.pri_ser_uuid)], options: nil)
    }
    
    func stopScan() {
        cbcManager.stopScan()
    }
    
    /**
     * Connect Lock Device
     */
    func connectDevice(lockDevice: LockDevice) {
        var exist: Bool = false
        for ld in lockDevices {
            if ld.identifier == lockDevice.identifier {
                exist = true
            }
        }
        if exist {
            isDisconnect = false
            lockDevice.delegate = self
            cbcManager.connect(lockDevice.peripheral!, options: nil)
        } else {
            delegate?.error(error: "Lock device not exist...")
        }
    }
    
    /**
     * Disonnect Lock Device
     */
    func disconnectDevice(lockDevice: LockDevice) {
        var exist: Bool = false
        for ld in lockDevices {
            if ld.identifier == lockDevice.identifier {
                exist = true
            }
        }
        if exist {
            isDisconnect = true
            cbcManager.cancelPeripheralConnection(lockDevice.peripheral!)
        } else {
            delegate?.error(error: "Lock device not exist...")
        }
    }
    
    func disconnectDevice(mac: String) {
        var hasMac:Bool = false
        for ld in lockDevices {
            if (ld.macAddress == mac) {
                isDisconnect = true
                cbcManager.cancelPeripheralConnection(ld.peripheral!)
                hasMac = true
                break
            }
        }
        if (!hasMac) {
            delegate?.error(error: "Lock device not exist...")
        }
    }
    
    func disconnectAll() {
        for ld in lockDevices {
            cbcManager.cancelPeripheralConnection(ld.peripheral!)
        }
        lockDevices = []
    }
    
    /**
     * Unlock Lock Device
     */
    func unlockDevice(lockDevice: LockDevice) {
        var exist: Bool = false
        for ld in lockDevices {
            if ld.identifier == lockDevice.identifier {
                exist = true
            }
        }
        if exist {
            lockDevice.unlock()
        } else {
            delegate?.error(error: "Lock device not exist...")
        }
    }
    
    func unlockDevice(mac: String, keyString: String) {
        var hasMac:Bool = false
        for ld in lockDevices {
            if (ld.macAddress == mac) {
                ld.password = keyString
                ld.unlock()
                hasMac = true
                break
            }
        }
        if (!hasMac) {
            delegate?.error(error: "Lock device not exist...")
        }
    }
    
    /**
     * Vibrate Device
     */
    func vibrateDevice(lockDevice: LockDevice) {
        var exist: Bool = false
        for ld in lockDevices {
            if ld.identifier == lockDevice.identifier {
                exist = true
            }
        }
        if exist {
            lockDevice.vibrate()
        } else {
            delegate?.error(error: "Lock device not exist...")
        }
    }
    
    func vibrateDevice(lockDevice: LockDevice, on: Bool) {
        var exist: Bool = false
        for ld in lockDevices {
            if ld.identifier == lockDevice.identifier {
                exist = true
            }
        }
        if exist {
            lockDevice.vibrate(on: on)
        } else {
            delegate?.error(error: "Lock device not exist...")
        }
    }
    
    /**
     * Change Device Password
     */
    func changeDevicePassword(lockDevice: LockDevice) {
        var exist: Bool = false
        for ld in lockDevices {
            if ld.identifier == lockDevice.identifier {
                exist = true
            }
        }
        if exist {
            lockDevice.changePassword()
        } else {
            delegate?.error(error: "Lock device not exist...")
        }
    }
    
    /**
     * Reset Device Password
     */
    func resetDevicePassword(lockDevice: LockDevice) {
        var exist: Bool = false
        for ld in lockDevices {
            if ld.identifier == lockDevice.identifier {
                exist = true
            }
        }
        if exist {
            lockDevice.resetPassword()
        } else {
            delegate?.error(error: "Lock device not exist...")
        }
    }
    
    func addLockDevice(peripheral: CBPeripheral) {
        let name = (peripheral.name?.description ?? "no_name").trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)
        let identifier = peripheral.identifier
        let state = peripheral.state.rawValue
        let newLockDevice = LockDevice(peripheral: peripheral, identifier: identifier.uuidString, name: name, state: state as NSNumber)
        if indexOfLockDevice(lockDevice: newLockDevice) == -1 {
            lockDevices.append(newLockDevice)
            if self.globelMac != nil {
                connectDevice(lockDevice: newLockDevice)
            }
        }
        delegate?.devices(didLockDeviceArrayUpdated: lockDevices)
        delegate?.device(didDiscoverDevice: newLockDevice)
    }
    
    func getLockDevice(peripheral: CBPeripheral) -> LockDevice? {
        for ld in lockDevices {
            if ld.peripheral == peripheral {
                return ld
            }
        }
        return nil
    }
    
    func indexOfLockDevice(lockDevice: LockDevice) -> Int{
        for (i, e) in lockDevices.enumerated() {
            if e.peripheral?.identifier == lockDevice.peripheral?.identifier {
                return i
            }
        }
        return -1
    }
}

extension SmartLockController: CBCentralManagerDelegate {
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if #available(iOS 10.0, *) {
            if central.state.rawValue == CBManagerState.poweredOn.rawValue {
                self.delegate?.bluetooth(isBluetoothAvailable: true)
            } else {
                self.delegate?.bluetooth(isBluetoothAvailable: false)
            }
        } else {
            // Fallback on earlier versions
        }
    }
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        addLockDevice(peripheral: peripheral)
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        let lockDevice = getLockDevice(peripheral: peripheral)
        if lockDevice != nil {
            if self.globelMac != nil {
                lockDevice!.connect(mac: self.globelMac!)
            } else {
                lockDevice!.connect()
            }
        }
    }
    
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        // Reconnect the device once it disconnect / disconnect means the lock open or no battery
        if (!isDisconnect) {
            cbcManager.connect(peripheral, options: nil)
        }
    }
    
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        print("didFailToConnect!!")
    }
}

extension SmartLockController: LockDeviceDelegate {
    func lockDevice(lockDevice: LockDevice, isTheMac state: Bool) {
        if state {
            print("mac address match")
        } else {
            self.disconnectDevice(lockDevice: lockDevice)
        }
    }
    func lockDevice(lockDevice: LockDevice, didOpen state: Bool) {
        if state {
            if self.globelMac != nil {
                // self.disconnectDevice(lockDevice: lockDevice)
            }
            delegate?.device(didOpen: lockDevice)
        } else {
            delegate?.device(didNotOpen: lockDevice)
        }
    }
    func lockDevice(lockDevice: LockDevice, didConnected state: Bool) {
        if state {
            if self.globelMac != nil {
                // unlockDevice(mac: self.globelMac!, keyString: self.globelKey!)
                delegate?.device(didConnectDevice: lockDevice)
            } else {
                delegate?.device(didConnectDevice: lockDevice)
            }
        }
    }
}

class SmartLockUtil {
    static let hexList = ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"]
    static let posiList = ["2", "3", "4", "5", "6", "7", "8", "9", "a"]
    
    static func get11Digits() -> Dictionary<String, NSMutableData> {
        // 4 position
        var dyPosiList: [String] = posiList
        var posiArr: [String] = []
        for _ in 0...3 {
            let rdmIdx = Int(arc4random_uniform(UInt32(dyPosiList.count)))
            let val = dyPosiList[rdmIdx]
            dyPosiList.remove(at: rdmIdx)
            posiArr.append(val)
        }
        // 18 digits
        var digitArr: [String] = []
        for _ in 0...17 {
            let rdmIdx = Int(arc4random_uniform(UInt32(hexList.count)))
            digitArr.append(hexList[rdmIdx])
        }
        // original 22 digits hex data / 11 bytes data
        let oriMData: NSMutableData = NSMutableData()
        for i in [0, 2] {
            let str = posiArr[i] + posiArr[i + 1]
            oriMData.append(hexStringToHex(string: str), length: 1)
        }
        for i in [0, 2, 4, 6, 8, 10, 12, 14, 16] {
            let str = digitArr[i] + digitArr[i + 1]
            oriMData.append(hexStringToHex(string: str), length: 1)
        }
        // get seeds
        var seedArr: [String] = []
        var digiPosiIdxArr: [Int] = [0, 1, 2, 3, 4, 5, 6, 7, 8]
        for i in posiArr {
            let idx = Int(i, radix: 16)
            digiPosiIdxArr.remove(at: digiPosiIdxArr.index(of: idx! - 2)!)
            seedArr.append(digitArr[(idx! - 2) * 2]+digitArr[(idx! - 2) * 2 + 1])
        }
        // get calculat numbers
        var calNumArr: [String] = []
        for i in digiPosiIdxArr {
            calNumArr.append(digitArr[i * 2] + digitArr[i * 2 + 1])
        }
        // crc16 calculation
        let seedDigiMData: NSMutableData = NSMutableData()
        for i in seedArr {
            seedDigiMData.append(hexStringToHex(string: i), length: 1)
        }
        let encyMData: NSMutableData = NSMutableData()
        for i in calNumArr {
            let crcDigiData: NSMutableData = NSMutableData()
            crcDigiData.append(hexStringToHex(string: i), length: 1)
            crcDigiData.append(seedDigiMData as Data)
            // CRC16
            let crc16DigiData = (crcDigiData as Data).crc16()
            encyMData.append(crc16DigiData)
        }
        let rtnDic: Dictionary = [
            "oriMData": oriMData,
            "encyMData": encyMData
        ]
        return rtnDic
    }
    
    static func get12Digits(data: Data) -> Dictionary<String, NSMutableData>  {
        let dataArr: [UInt8] = data.bytes
        let byteOri: [UInt8] = [dataArr[3], dataArr[4], dataArr[5], dataArr[6], dataArr[7], dataArr[8], dataArr[10], dataArr[12], dataArr[14], dataArr[16], dataArr[18]]
        let oriMData: NSMutableData = NSMutableData()
        oriMData.append(byteOri, length: 11)
        let encyMData: NSMutableData = NSMutableData()
        for i in 0...5 {
            let byteCrc: [UInt8] = [dataArr[i + 3], dataArr[10], dataArr[12], dataArr[14], dataArr[16], dataArr[18]]
            let crcDigiData: NSMutableData = NSMutableData(bytes: byteCrc, length: 6)
            // CRC16
            let crc16DigiData = (crcDigiData as Data).crc16()
            encyMData.append(crc16DigiData)
        }
        let rtnDic: Dictionary = [
            "oriMData": oriMData,
            "encyMData": encyMData
        ]
        return rtnDic
    }
    
    static func hexStringToHex(string: String) -> [UInt8] {
        let chars = Array(string.characters)
        let numbers = stride(from: 0, to: chars.count, by: 2).map {
            UInt8(String(chars[$0 ..< $0+2]), radix: 16) ?? 0
        }
        return numbers
    }
}

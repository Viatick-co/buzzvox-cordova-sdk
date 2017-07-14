//
//  ViatickLockSDK.swift
//  ViaUnlock
//
//  Created by Bie Yaqing on 18/2/17.
//  Copyright © 2017 Bie Yaqing. All rights reserved.
//
import Foundation

protocol ViatickServiceDelegate {
    func service(request: URLRequest, getKeyString keyString: String, mac: String)
    func service(request: URLRequest, appInitSuccess message: String, isAppInitCheck: Bool, isOutsetLock: Bool, isUnlocking: Bool, accountId: NSNumber, bookingId: NSNumber, authKey: String)
    func service(request: URLRequest, appInitError message: String, isAppInitCheck: Bool, isOutsetLock: Bool, isUnlocking: Bool, accountId: NSNumber, bookingId: NSNumber, authKey: String)
    func service(request: URLRequest, getBookingInfo lockId: NSNumber, buzzvoxUserId: NSNumber, buzzvoxBookingId: NSNumber, userId: NSNumber, bookingId: NSNumber, authKey: String, isOutsetLock: Bool, isUnlocking: Bool)
    func service(request: URLRequest, getLockInfo mac: String, userId: NSNumber, bookingId: NSNumber, authKey: String, isOutsetLock: Bool, isUnlocking: Bool)
    func service(request: URLRequest, error message: String)
    func service(request: URLRequest, startTrip isSuccess: Bool, errorMessage: String)
    func service(request: URLRequest, endTrip isSuccess: Bool, errorMessage: String)
}

class ViatickService {
    let buzzVoxEndPoint = "https://buzzvox.co"
    let popScootEndPoint = "http://test.popscoot.com/popscoot/api"
    
    var delegate: ViatickServiceDelegate?
    
    var masterId: NSNumber?
    var masterSecret: String? = nil
    
    func getKeyString(bookingId: NSNumber, mac: String) {
        print("bookingId", bookingId)
        
        var request = URLRequest(url: URL(string: "\(buzzVoxEndPoint)/blesys/service/collect/lockkey/\(self.masterId!.stringValue)/\(bookingId.stringValue)/\(mac)?_=\(String(NSDate().timeIntervalSince1970))")!)
        request.httpMethod = "GET"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(self.masterSecret!, forHTTPHeaderField: "auth_secret")
        
        print("request", request)
        
        let task = URLSession.shared.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {
                print("error: \(error)")
                return
            }
            if let httpStatus = response as? HTTPURLResponse {
                switch httpStatus.statusCode {
                case 200:
                    do {
                        let responseData = try JSONSerialization.jsonObject(with: data, options: []) as! [String:Any]
                        let status: NSNumber = responseData["status"] as! NSNumber
                        if (status == 1) {
                            let data: [String: Any] = responseData["data"] as! [String: Any]
                            let keyString: String = data["keyString"] as! String
                            print("keyString", keyString)
                            self.delegate?.service(request: request, getKeyString: keyString, mac: mac)
                        } else {
                            let message: String = responseData["message"] as! String
                            print("message", message)
                            self.delegate?.service(request: request, error: message)
                        }
                    } catch {
                        print("Unparsable JSON...")
                        self.delegate?.service(request: request, error: "Unparsable JSON...")
                    }
                    break
                default:
                    print(httpStatus.statusCode)
                    self.delegate?.service(request: request, error: httpStatus.statusCode.description)
                    break
                }
            }
        }
        task.resume()
    }
    
    func appInit(userId: NSNumber, isAppInitCheck: Bool, isOutsetLock: Bool, isUnlocking: Bool, bookingId: NSNumber, authKey: String) {
        var request = URLRequest(url: URL(string: popScootEndPoint + "/extra/init" + "?_=" + String(NSDate().timeIntervalSince1970))!)
        request.httpMethod = "GET"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        let task = URLSession.shared.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {
                print("error: \(error)")
                return
            }
            if let httpStatus = response as? HTTPURLResponse {
                switch httpStatus.statusCode {
                case 200:
                    do {
                        let responseData = try JSONSerialization.jsonObject(with: data, options: []) as! [String:Any]
                        
                        let status: NSNumber = responseData["status"] as! NSNumber
                        if (status == 1) {
                            self.masterId = (responseData["masterId"] as! NSNumber)
                            self.masterSecret = (responseData["masterSecret"] as! String)
                            
                            print("status: \(status)")
                            self.delegate?.service(request: request, appInitSuccess: "Success", isAppInitCheck: isAppInitCheck, isOutsetLock: isOutsetLock, isUnlocking: isUnlocking, accountId: userId, bookingId: bookingId, authKey: authKey)
                        } else {
                            let message: String = responseData["message"] as! String
                            print("message", message)
                            self.delegate?.service(request: request, appInitError: message, isAppInitCheck: isAppInitCheck, isOutsetLock: isOutsetLock, isUnlocking: isUnlocking, accountId: userId, bookingId: bookingId, authKey: authKey)
                        }
                    } catch {
                        self.delegate?.service(request: request, appInitError: "Unparsable JSON...", isAppInitCheck: isAppInitCheck, isOutsetLock: isOutsetLock, isUnlocking: isUnlocking, accountId: userId, bookingId: bookingId, authKey: authKey)
                    }
                    break
                default:
                    print(httpStatus.statusCode)
                    let responseData = String(data: data, encoding: String.Encoding.utf8)?.replacingOccurrences(of: "\"", with: "", options: NSString.CompareOptions.literal, range:nil)
                    self.delegate?.service(request: request, appInitError: responseData!, isAppInitCheck: isAppInitCheck, isOutsetLock: isOutsetLock, isUnlocking: isUnlocking, accountId: userId, bookingId: bookingId, authKey: authKey)
                    break
                }
            }
        }
        task.resume()
    }
    
    func getBookingInfo(accountId: NSNumber, bookingId: NSNumber, authKey: String, isOutsetLock: Bool, isUnlocking: Bool) {
        var request = URLRequest(url: URL(string: "\(popScootEndPoint)/accounts/\(accountId)/bookings/\(bookingId)?_=\(String(NSDate().timeIntervalSince1970))")!)
        print(request)
        request.httpMethod = "GET"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(authKey, forHTTPHeaderField: "Auth-Secret")
        let task = URLSession.shared.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {
                print("error: \(error)")
                return
            }
            if let httpStatus = response as? HTTPURLResponse {
                switch httpStatus.statusCode {
                case 200:
                    do {
                        let responseData = try JSONSerialization.jsonObject(with: data, options: []) as! [String:Any]
                        
                        var lockId: NSNumber = 0
                        if isOutsetLock {
                           lockId = responseData["outsetLockId"] as! NSNumber
                        } else {
                           lockId = responseData["destinationLockId"] as! NSNumber
                        }
                         
                        let integrateId: String = responseData["integrateId"] as! String
                        let startIndex = integrateId.index(integrateId.startIndex, offsetBy: 13)
                        let buzzvoxBookingId: NSNumber = NSNumber(value: Int(integrateId.substring(from: startIndex))!)
                        
                        print("buzzvoxBookingId", buzzvoxBookingId)
                        self.delegate?.service(request: request, getBookingInfo: lockId, buzzvoxUserId: self.masterId!, buzzvoxBookingId: buzzvoxBookingId,
                                        userId: accountId, bookingId: bookingId, authKey: authKey, isOutsetLock: isOutsetLock, isUnlocking: isUnlocking)
                    } catch {
                        print("Unparsable JSON...")
                        self.delegate?.service(request: request, error: "Unparsable JSON...")
                    }
                    break
                default:
                    print(httpStatus.statusCode)
                    let responseData = String(data: data, encoding: String.Encoding.utf8)?.replacingOccurrences(of: "\"", with: "", options: NSString.CompareOptions.literal, range:nil)
                    self.delegate?.service(request: request, error: responseData!)
                    break
                }
            }
        }
        task.resume()
    }
    
    func getLockInfo(lockId: NSNumber, buzzvoxUserId: NSNumber, buzzvoxBookingId: NSNumber, userId: NSNumber, bookingId: NSNumber, authKey: String, isOutsetLock: Bool, isUnlocking: Bool) {
        var request = URLRequest(url: URL(string: buzzVoxEndPoint + "/blesys/service/res/lite-bicycle-locks/" + String(describing: lockId) + "?_=" + String(NSDate().timeIntervalSince1970))!)
        request.httpMethod = "GET"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(self.masterSecret!, forHTTPHeaderField: "auth_secret")
        
        let task = URLSession.shared.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {
                print("error: \(error)")
                return
            }
            if let httpStatus = response as? HTTPURLResponse {
                switch httpStatus.statusCode {
                case 200:
                    do {
                        let responseData = try JSONSerialization.jsonObject(with: data, options: []) as! [String:Any]
                        let status: NSNumber = responseData["status"] as! NSNumber
                        if (status == 1) {
                            let data: [String: Any] = responseData["data"] as! [String: Any]
                            let mac: String = data["mac"] as! String
                            
                            print("mac", mac)
                            
                            self.delegate?.service(request: request, getLockInfo: mac, userId: userId, bookingId: bookingId, authKey: authKey, isOutsetLock: isOutsetLock, isUnlocking: isUnlocking)
                        } else {
                            let message: String = responseData["message"] as! String
                            print("message", message)
                            self.delegate?.service(request: request, error: message)
                        }
                    } catch {
                        print("Unparsable JSON...")
                        self.delegate?.service(request: request, error: "Unparsable JSON...")
                    }
                    break
                default:
                    print(httpStatus.statusCode)
                    self.delegate?.service(request: request, error: httpStatus.statusCode.description)
                    break
                }
            }
        }
        task.resume()
    }
    
    func startTrip(accountId: NSNumber, bookingId: NSNumber, authKey: String) {
        var request = URLRequest(url: URL(string: "\(popScootEndPoint)/trip")!)
        
        let json: [String: Any] = ["accountId": accountId,
                                   "bookingId": bookingId,
                                   "action": "start"]
        let jsonData = try? JSONSerialization.data(withJSONObject: json)
        
        print(request)
        request.httpMethod = "PUT"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(authKey, forHTTPHeaderField: "Auth-Secret")
        request.httpBody = jsonData
        
        let task = URLSession.shared.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {
                print("error: \(error)")
                return
            }
            if let httpStatus = response as? HTTPURLResponse {
                switch httpStatus.statusCode {
                case 200:
                    do {
                        self.delegate?.service(request: request, startTrip: true, errorMessage: "")
                    } catch {
                        self.delegate?.service(request: request, startTrip: false, errorMessage: "Unparsable JSON...")
                    }
                    break
                default:
                    print(httpStatus.statusCode)
                    let responseData = String(data: data, encoding: .utf8)?.replacingOccurrences(of: "\"", with: "", options: NSString.CompareOptions.literal, range:nil)
                    self.delegate?.service(request: request, startTrip: false, errorMessage: responseData!)
                    break
                }
            }
        }
        task.resume()
    }
    
    func endTrip(accountId: NSNumber, bookingId: NSNumber, authKey: String) {
        var request = URLRequest(url: URL(string: "\(popScootEndPoint)/trip")!)
        
        let json: [String: Any] = ["accountId": accountId,
                                   "bookingId": bookingId,
                                   "action": "end"]
        let jsonData = try? JSONSerialization.data(withJSONObject: json)
        
        print(request)
        request.httpMethod = "PUT"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(authKey, forHTTPHeaderField: "Auth-Secret")
        request.httpBody = jsonData
        
        let task = URLSession.shared.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {
                print("error: \(error)")
                return
            }
            if let httpStatus = response as? HTTPURLResponse {
                switch httpStatus.statusCode {
                case 200:
                    do {
                        self.delegate?.service(request: request, endTrip: true, errorMessage: "")
                    } catch {
                        self.delegate?.service(request: request, endTrip: false, errorMessage: "Unparsable JSON...")
                    }
                    break
                default:
                    print(httpStatus.statusCode)
                    let responseData = String(data: data, encoding: String.Encoding.utf8)?.replacingOccurrences(of: "\"", with: "", options: NSString.CompareOptions.literal, range:nil)
                    self.delegate?.service(request: request, endTrip: false, errorMessage: responseData!)
                    break
                }
            }
        }
        task.resume()
    }

}

protocol ViatickLockControllerDelegate: class {
    func lockDevice(didOpenLock lockDevice: LockDevice)
    func lockDevice(didNotOpenLock lockDevice: LockDevice)
    func error(error message: String)
    func appInitSuccess()
    func appInitError(error message: String)
    func startTrip(isSuccess: Bool, errorMessage: String)
    func endTrip(isSuccess: Bool, errorMessage: String)
}

class ViatickLockController {
    static let sharedInstance = ViatickLockController()
    
    let smartLockController: SmartLockController = SmartLockController.sharedInstance
    
    let viatickService: ViatickService = ViatickService()
    
    weak var delegate: ViatickLockControllerDelegate?
    
    var bookingId: NSNumber = 0
    var lockUnlocked: Bool = false
    
    func scanDevices() {
        smartLockController.initController()
        smartLockController.delegate = self
    }
    
    func scanConnectDevice(mac: String) {
        lockUnlocked = false
        smartLockController.initController(mac: mac)
        smartLockController.delegate = self
    }
    
    func connect2GetMac(lockDevice: LockDevice) {
        smartLockController.connectDevice(lockDevice: lockDevice)
    }
    
    func openSmartLock(mac: String) {
        viatickService.delegate = self
        getLockKey(mac: mac)
    }
    
    func getLockKey(mac: String) {
        viatickService.delegate = self
        viatickService.getKeyString(bookingId: self.bookingId, mac: mac)
    }
    
    func appInit(userId: NSNumber) {
        viatickService.delegate = self
        viatickService.appInit(userId: userId, isAppInitCheck: false, isOutsetLock: false, isUnlocking: false, bookingId: 0, authKey: "")
    }
    
    func appInitCheck(accountId: NSNumber, bookingId: NSNumber, authKey: String, isOutsetLock: Bool, isUnlocking: Bool) {
        print("appInitCheck...")
        if viatickService.masterSecret != nil {
            getBookingInfo(accountId: accountId, bookingId: bookingId, authKey: authKey, isOutsetLock: isOutsetLock, isUnlocking: isUnlocking)
        } else {
            print("appInitCheck")
            viatickService.delegate = self
            viatickService.appInit(userId: accountId, isAppInitCheck: true, isOutsetLock: isOutsetLock, isUnlocking: isUnlocking, bookingId: bookingId, authKey: authKey)
        }
    }
    
    func getBookingInfo(accountId: NSNumber, bookingId: NSNumber, authKey: String, isOutsetLock: Bool, isUnlocking: Bool) {
        viatickService.delegate = self
        viatickService.getBookingInfo(accountId: accountId, bookingId: bookingId, authKey: authKey, isOutsetLock: isOutsetLock, isUnlocking: isUnlocking)
    }
    
    func getLockInfo(lockId: NSNumber, buzzvoxUserId: NSNumber, buzzvoxBookingId: NSNumber, userId: NSNumber, bookingId: NSNumber, authKey: String, isOutsetLock: Bool, isUnlocking: Bool) {
        self.bookingId = buzzvoxBookingId
        
        viatickService.delegate = self
        viatickService.getLockInfo(lockId: lockId, buzzvoxUserId: buzzvoxUserId, buzzvoxBookingId: buzzvoxBookingId, userId: userId, bookingId: bookingId, authKey: authKey, isOutsetLock: isOutsetLock, isUnlocking: isUnlocking)
    }
    
    func openLockAction(mac:String, keyString: String) {
        viatickService.delegate = self
        smartLockController.unlockDevice(mac: mac, keyString: keyString)
    }
    
    func disconnect(mac: String) {
        smartLockController.disconnectDevice(mac: mac)
    }
    
    func startTrip(mac: String, userId: NSNumber, bookingId: NSNumber, authKey: String) {
        let lockDevice = smartLockController.getLockDevice(mac: mac)
        
        if (lockDevice != nil && smartLockController.checkConnection(lockDevice: lockDevice!)) {
            viatickService.delegate = self
            viatickService.startTrip(accountId: userId, bookingId: bookingId, authKey: authKey)
        } else {
            delegate?.startTrip(isSuccess: false, errorMessage: "Lock isn't connected")
        }
    }
    
    func endTrip(mac: String, userId: NSNumber, bookingId: NSNumber, authKey: String) {
        let lockDevice = smartLockController.getLockDevice(mac: mac)
        
        if (lockDevice != nil && smartLockController.checkConnection(lockDevice: lockDevice!)) {
            viatickService.delegate = self
            viatickService.endTrip(accountId: userId, bookingId: bookingId, authKey: authKey)
        } else {
            delegate?.endTrip(isSuccess: false, errorMessage: "Lock isn't connected")
        }
    }
}

extension ViatickLockController: SmartLockControllerDelegate {
    func bluetooth(isBluetoothAvailable state: Bool) {
        if (state) {
            smartLockController.startScan()
        }
    }
    
    func bluetooth(isInitialised state: Bool) {
        print("bluetooth initialised")
        smartLockController.startScan()
    }
    
    func device(didDiscoverDevice lockDevice: LockDevice) {
        print("device discovered", lockDevice.identifier)
        connect2GetMac(lockDevice: lockDevice)
    }
    
    func device(didConnectDevice lockDevice: LockDevice) {
        print("device connected")
//        lockDevice.connected = true
        
        if !lockUnlocked {
            getLockKey(mac: lockDevice.macAddress)
        }
    }
    
    func devices(didLockDeviceArrayUpdated lockDevices: [LockDevice]) {
        // Nothing
    }
    
    func device(didOpen lockDevice: LockDevice) {
        lockUnlocked = true
//        lockDevice.connected = false
        delegate?.lockDevice(didOpenLock: lockDevice)
    }
    
    func device(didNotOpen lockDevice: LockDevice) {
        delegate?.lockDevice(didNotOpenLock: lockDevice)
    }
    
    func error(error message: String) {
        delegate?.error(error: message)
    }
}

extension ViatickLockController: ViatickServiceDelegate {
    func service(request: URLRequest, appInitSuccess message: String, isAppInitCheck: Bool, isOutsetLock: Bool, isUnlocking: Bool, accountId: NSNumber, bookingId: NSNumber, authKey: String) {
        if !isAppInitCheck {
            delegate?.appInitSuccess()
        } else {
            getBookingInfo(accountId: accountId, bookingId: bookingId, authKey: authKey, isOutsetLock: isOutsetLock, isUnlocking: isUnlocking)
        }
    }
    
    func service(request: URLRequest, appInitError message: String, isAppInitCheck: Bool, isOutsetLock: Bool, isUnlocking: Bool, accountId: NSNumber, bookingId: NSNumber, authKey: String) {
        if !isAppInitCheck {
            delegate?.appInitError(error: message)
        } else {
            if isUnlocking {
                delegate?.error(error: message)
            } else {
                if isOutsetLock {
                    delegate?.startTrip(isSuccess: false, errorMessage: "Could not retrieve app info")
                } else {
                    delegate?.endTrip(isSuccess: false, errorMessage: "Could not retrieve app info")
                }
            }
        }
    }
    
    func service(request: URLRequest, getKeyString keyString: String, mac: String) {
        openLockAction(mac: mac, keyString: keyString)
    }
    
    func service(request: URLRequest, getBookingInfo lockId: NSNumber, buzzvoxUserId: NSNumber, buzzvoxBookingId: NSNumber, userId: NSNumber,
                 bookingId: NSNumber, authKey: String, isOutsetLock: Bool, isUnlocking: Bool) {
        print("getBookingInfo", "success");
        getLockInfo(lockId: lockId, buzzvoxUserId: buzzvoxUserId, buzzvoxBookingId: buzzvoxBookingId, userId: userId, bookingId: bookingId, authKey: authKey,
                    isOutsetLock: isOutsetLock, isUnlocking: isUnlocking)
    }
    
    func service(request: URLRequest, getLockInfo mac: String, userId: NSNumber, bookingId: NSNumber, authKey: String, isOutsetLock: Bool, isUnlocking: Bool) {
        print("getLockInfo", mac);
        print("isUnlocking", isUnlocking)
        print("isOutsetLock", isOutsetLock)
        
        if isUnlocking {
            scanConnectDevice(mac: mac)
        } else {
            if isOutsetLock {
                disconnect(mac: mac)
                startTrip(mac: mac, userId: userId, bookingId: bookingId, authKey: authKey)
            } else {
                disconnect(mac: mac)
                endTrip(mac: mac, userId: userId, bookingId: bookingId, authKey: authKey)
            }
        }
    }
    
    func service(request: URLRequest, error message: String) {
        print("message", message)
    }
    
    func service(request: URLRequest, startTrip isSuccess: Bool, errorMessage: String) {
        delegate?.startTrip(isSuccess: isSuccess, errorMessage: errorMessage)
    }
    
    func service(request: URLRequest, endTrip isSuccess: Bool, errorMessage: String) {
        delegate?.endTrip(isSuccess: isSuccess, errorMessage: errorMessage)
    }
}

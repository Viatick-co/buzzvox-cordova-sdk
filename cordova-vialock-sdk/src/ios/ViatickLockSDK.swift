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
    func service(request: URLRequest, error message: String)
}

class ViatickService {
    let endpoint = "https://buzzvox.co"

    var delegate: ViatickServiceDelegate?

    func getKeyString(userId: NSNumber, bookingId: NSNumber, mac: String, authKey: String) {
        var request = URLRequest(url: URL(string: endpoint+"/blesys/service/collect/lockkey/"+userId.stringValue+"/"+bookingId.stringValue+"/"+mac+"?_="+String(NSDate().timeIntervalSince1970))!)
        request.httpMethod = "GET"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(authKey, forHTTPHeaderField: "auth_secret")
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
}

protocol ViatickLockControllerDelegate: class {
    func lockDevice(didConnectLock lockDevice: LockDevice)
    func lockDevice(didOpenLock lockDevice: LockDevice)
    func lockDevice(didNotOpenLock lockDevice: LockDevice)
    func error(error message: String)
}

class ViatickLockController {
    static let sharedInstance = ViatickLockController()

    let smartLockController: SmartLockController = SmartLockController.sharedInstance

    let viatickService: ViatickService = ViatickService()

    weak var delegate: ViatickLockControllerDelegate?

    func scanDevices() {
        smartLockController.initController()
        smartLockController.delegate = self
    }

    func scanConnectDevice(mac: String) {
        smartLockController.initController(mac: mac)
        smartLockController.delegate = self
    }

    func connect2GetMac(lockDevice: LockDevice) {
        smartLockController.connectDevice(lockDevice: lockDevice)
    }

    func openSmartLock(userId: NSNumber, bookingId: NSNumber, mac: String, authKey: String) {
        viatickService.delegate = self
        getLockKey(userId: userId, bookingId: bookingId, mac: mac, authKey: authKey)
    }

    func getLockKey(userId: NSNumber, bookingId: NSNumber, mac: String, authKey: String) {
        viatickService.getKeyString(userId: userId, bookingId: bookingId, mac: mac, authKey: authKey)
    }

    func openLockAction(mac:String, keyString: String) {
        smartLockController.unlockDevice(mac: mac, keyString: keyString)
    }

    func disconnect(mac: String) {
        smartLockController.disconnectDevice(mac: mac)
    }
}

extension ViatickLockController: SmartLockControllerDelegate {
    func bluetooth(isBluetoothAvailable state: Bool) {
        if (state) {
            smartLockController.startScan()
        }
    }

    func bluetooth(isInitialised state: Bool) {
        smartLockController.startScan()
    }

    func device(didDiscoverDevice lockDevice: LockDevice) {
        connect2GetMac(lockDevice: lockDevice)
    }

    func device(didConnectDevice lockDevice: LockDevice) {
        delegate?.lockDevice(didConnectLock: lockDevice)
    }

    func devices(didLockDeviceArrayUpdated lockDevices: [LockDevice]) {
        // Nothing
    }

    func device(didOpen lockDevice: LockDevice) {
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
    func service(request: URLRequest, getKeyString keyString: String, mac: String) {
        openLockAction(mac: mac, keyString: keyString)
    }

    func service(request: URLRequest, error message: String) {
        print("message", message)
    }
}

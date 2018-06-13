//
//  ViaLockPlugin.swift
//  BuzzVox
//
//  Created by Viatick on 3/13/17.
//
//

import Foundation
@objc(BVViaLockPlugin) class ViaLockPlugin : CDVPlugin {
    var viatickLockController: ViatickLockController? = nil
    var delegateCallbackIdInit: String? = nil
    var delegateCallbackIdOpen: String? = nil
    var delegateCallbackIdDisconnect: String? = nil

    func viaLockInit(_ command:CDVInvokedUrlCommand) {
        let mac: String = command.arguments[0] as! String
        viatickLockController = ViatickLockController.sharedInstance
        viatickLockController?.delegate = self
        viatickLockController?.scanConnectDevice(mac: mac)
        
        delegateCallbackIdInit = command.callbackId
    }
    
    func viaLockOpen(_ command:CDVInvokedUrlCommand) {
        let userId: NSNumber = command.arguments[0] as! NSNumber
        let bookingId: NSNumber = command.arguments[1] as! NSNumber
        let mac: String = command.arguments[2] as! String
        let authKey: String = command.arguments[3] as! String

        viatickLockController?.openSmartLock(userId: userId, bookingId: bookingId, mac: mac, authKey: authKey)
        delegateCallbackIdOpen = command.callbackId
    }
    
    func viaLockDisconnect(_ command:CDVInvokedUrlCommand) {
        let mac: String = command.arguments[0] as! String
        
        viatickLockController?.disconnect(mac: mac)
        delegateCallbackIdDisconnect = command.callbackId
    }
}

extension ViaLockPlugin: ViatickLockControllerDelegate {
    func lockDevice(didConnectLock lockDevice: LockDevice) {
        var pluginResult: CDVPluginResult?
        pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: lockDevice.description)
        pluginResult?.setKeepCallbackAs(true)
        self.commandDelegate.send(pluginResult, callbackId: delegateCallbackIdInit)
    }
    
    func lockDevice(didOpenLock lockDevice: LockDevice) {
        var pluginResult: CDVPluginResult?
        pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: lockDevice.description)
        pluginResult?.setKeepCallbackAs(true)
        self.commandDelegate.send(pluginResult, callbackId: delegateCallbackIdOpen)
    }
    
    func lockDevice(didNotOpenLock lockDevice: LockDevice) {
        var pluginResult: CDVPluginResult?
        
        var error: String = String()
        error = "Could not unlock this bicycle lock"
        pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: error)
        pluginResult?.setKeepCallbackAs(true)
        self.commandDelegate.send(pluginResult, callbackId: delegateCallbackIdOpen)
    }
    
    func error(error message: String) {
        print(message)
        
        var pluginResult: CDVPluginResult?
        pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: message)
        self.commandDelegate.send(pluginResult, callbackId: delegateCallbackIdOpen)
    }
}

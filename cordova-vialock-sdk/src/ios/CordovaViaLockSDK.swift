import Foundation
@objc(CordovaViaLockSDK) class ViaLockPlugin : CDVPlugin {
    var viatickLockController: ViatickLockController? = nil
    var delegateCallbackIdInit: String? = nil
    var delegateCallbackIdUnlock: String? = nil
    var delegateCallbackIdStartTrip: String? = nil
    var delegateCallbackIdEndTrip: String? = nil
    
    override func pluginInitialize() {
        viatickLockController = ViatickLockController.sharedInstance
        viatickLockController?.delegate = self
    }

    func initiate(_ command:CDVInvokedUrlCommand) {
        let accountId: NSNumber = command.arguments[0] as! NSNumber
        viatickLockController?.appInit(userId: accountId)

        delegateCallbackIdInit = command.callbackId
    }

    func unlockOutsetLock(_ command:CDVInvokedUrlCommand) {
        let accountId: NSNumber = command.arguments[0] as! NSNumber
        let bookingId: NSNumber = command.arguments[1] as! NSNumber
        let authKey: String = command.arguments[2] as! String
        
        viatickLockController?.appInitCheck(accountId: accountId, bookingId: bookingId, authKey: authKey, isOutsetLock: true, isUnlocking: true)
        delegateCallbackIdUnlock = command.callbackId
    }
    
    func unlockDestinationLock(_ command:CDVInvokedUrlCommand) {
        let accountId: NSNumber = command.arguments[0] as! NSNumber
        let bookingId: NSNumber = command.arguments[1] as! NSNumber
        let authKey: String = command.arguments[2] as! String
        
        viatickLockController?.appInitCheck(accountId: accountId, bookingId: bookingId, authKey: authKey, isOutsetLock: false, isUnlocking: true)
        delegateCallbackIdUnlock = command.callbackId
    }
    
//    func startTrip(_ command:CDVInvokedUrlCommand) {
//        let accountId: NSNumber = command.arguments[0] as! NSNumber
//        let bookingId: NSNumber = command.arguments[1] as! NSNumber
//        let authKey: String = command.arguments[2] as! String
//        
//        viatickLockController?.appInitCheck(accountId: accountId, bookingId: bookingId, authKey: authKey, isOutsetLock: true, isUnlocking: false)
//        delegateCallbackIdStartTrip = command.callbackId
//    }
//    
//    func endTrip(_ command:CDVInvokedUrlCommand) {
//        let accountId: NSNumber = command.arguments[0] as! NSNumber
//        let bookingId: NSNumber = command.arguments[1] as! NSNumber
//        let authKey: String = command.arguments[2] as! String
//        viatickLockController?.appInitCheck(accountId: accountId, bookingId: bookingId, authKey: authKey, isOutsetLock: false, isUnlocking: false)
//        delegateCallbackIdEndTrip = command.callbackId
//    }
}

extension ViaLockPlugin: ViatickLockControllerDelegate {
    func lockDevice(didOpenLock lockDevice: LockDevice) {
        var pluginResult: CDVPluginResult?
        pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: lockDevice.description)
        pluginResult?.setKeepCallbackAs(true)
        self.commandDelegate.send(pluginResult, callbackId: delegateCallbackIdUnlock)
    }

    func lockDevice(didNotOpenLock lockDevice: LockDevice) {
        var pluginResult: CDVPluginResult?

        var error: String = String()
        error = "Could not unlock this lock"
        pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: error)
        pluginResult?.setKeepCallbackAs(true)
        self.commandDelegate.send(pluginResult, callbackId: delegateCallbackIdUnlock)
    }

    func error(error message: String) {
        print(message)

        var pluginResult: CDVPluginResult?
        pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: message)
        self.commandDelegate.send(pluginResult, callbackId: delegateCallbackIdUnlock)
    }
    
    func appInitSuccess() {
        print("success")
        
        var pluginResult: CDVPluginResult?
        pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: "Success")
        self.commandDelegate.send(pluginResult, callbackId: delegateCallbackIdInit)
    }
    
    func appInitError(error message: String) {
        print(message)
        
        var pluginResult: CDVPluginResult?
        pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: message)
        self.commandDelegate.send(pluginResult, callbackId: delegateCallbackIdInit)
    }
    
    func startTrip(isSuccess: Bool, errorMessage: String) {
        print(errorMessage)
        
        var pluginResult: CDVPluginResult?
        
        if isSuccess {
            pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: "Success")
        } else {
            pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: errorMessage)
        }
        pluginResult?.setKeepCallbackAs(false)
        
        self.commandDelegate.send(pluginResult, callbackId: delegateCallbackIdUnlock)
    }
    
    func endTrip(isSuccess: Bool, errorMessage: String) {
        print(errorMessage)
        
        var pluginResult: CDVPluginResult?
        
        if isSuccess {
            pluginResult = CDVPluginResult(status:CDVCommandStatus_OK, messageAs: "Success")
        } else {
            pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: errorMessage)
        }
        pluginResult?.setKeepCallbackAs(false)
        
        self.commandDelegate.send(pluginResult, callbackId: delegateCallbackIdUnlock)
    }
}

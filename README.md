### Dependency

* iOS: CryptoSwift `crc16`
* Android: none

### How To Install
1 - Clone the project

2 - Copy the folder `cordova-vialock-sdk` into the `plugins` folder of your Cordova project (if the folder doesn't exist, create one)

3 - Add this line into `config.xml`:
```xml
<plugin name="cordova-vialock-sdk" spec="~0.0.1" />
```

4 - Run this command in the root folder of your project:
```cmd
cordova plugin add cordova-vialock-sdk
```

5 - For iOS, you need to import `Cryptoswift` as an embedded framework in your project based on the instruction here:
https://github.com/krzyzanowskim/CryptoSwift

> Noted: If the Swift version is 3.2 or below you should use CryptoSwift version 0.7.0. Use the latest version for Swift 4. More details here:
https://github.com/krzyzanowskim/CryptoSwift/issues/459

### Available methods

```javascript
1- viaLockInit: connect to the lock of a given mac address
cordova.plugins.CordovaViaLockSDK.viaLockInit(macAddress,
  function (success) {
    // Do when success
  }, function (error) {
    // Do when error
});
```

2- viaLockOpen: unlock the lock
```javascript
cordova.plugins.CordovaViaLockSDK.viaLockOpen(accountId, bookingId, macAddress, authSecret, function (success) {
    // Do when success
  }, function (error) {
    // Do when error
});
```

3- viaLockDisconnect: disconnect with the lock of a given mac address
```javascript
cordova.plugins.CordovaViaLockSDK.viaLockDisconnect(macAddress,
  function (success) {
    // Do when success
  }, function (error) {
    // Do when error
});
```

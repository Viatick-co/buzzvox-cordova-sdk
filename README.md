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

### Sample Code

```javascript
  /*
   * Popscoot's Auth-Secret of the current user
   */
  var authKey = "_p11cf18gt9hoj11500000430883kcdova7s7i5c0sng"

  /*
   * Initiate the SDK, accountId is required so should only be triggered for logged in user
   */
  $scope.initiate = function (accountId) {
    console.info('initiate()','initiating...');

    cordova.plugins.CordovaViaLockSDK.initiate(accountId)
    .then(function () {
      console.info('cordova.plugins.CordovaViaLockSDK.initiate()', 'success');
    }, function (error) {
      console.error('cordova.plugins.CordovaViaLockSDK.initiate()', error);
    });
  };

 /*
  * Connect and unlock the outset lock. After the lock was unlocked, the trip will be automatically started
  */
  $scope.unlockOutsetLock = function (accountId, bookingId) {
    cordova.plugins.CordovaViaLockSDK.unlockOutsetLock(accountId, bookingId, authKey)
    .then(function (success) {
      console.info('cordova.plugins.CordovaViaLockSDK.unlockOutsetLock()', 'Success');
    }, function (error) {
      console.error('cordova.plugins.CordovaViaLockSDK.unlockOutsetLock()', error);
    });
  };

 /*
  * Connect and unlock the destination lock
  * After the user closes the lock, the trip will be automatically ended
  */
  $scope.unlockDestinationLock = function (accountId, bookingId) {
    cordova.plugins.CordovaViaLockSDK.unlockDestinationLock(accountId, bookingId, authKey)
    .then(function (success) {
      console.info('cordova.plugins.CordovaViaLockSDK.unlockDestinationLock()', 'Success');
    }, function (error) {
      console.error('cordova.plugins.CordovaViaLockSDK.unlockDestinationLock()', error);
    });
  };
```

var exec = require('cordova/exec');

exports.initiate = function (accountId) {
  return new Promise(function (success, error) {
    exec(success, error, "CordovaViaLockSDK", "initiate", [accountId]);
  });
};

exports.unlockOutsetLock = function (accountId, bookingId, authKey) {
  return new Promise(function (success, error) {
    exec(success, error, "CordovaViaLockSDK", "unlockOutsetLock", [accountId, bookingId, authKey]);
  });
};

// exports.startTrip = function (accountId, bookingId, authKey) {
//   return new Promise(function (success, error) {
//     exec(success, error, "CordovaViaLockSDK", "startTrip", [accountId, bookingId, authKey]);
//   });
// };

exports.unlockDestinationLock = function (accountId, bookingId, authKey) {
  return new Promise(function (success, error) {
    exec(success, error, "CordovaViaLockSDK", "unlockDestinationLock", [accountId, bookingId, authKey]);
  });
};

// exports.endTrip = function (accountId, bookingId, authKey) {
//   return new Promise(function (success, error) {
//     exec(success, error, "CordovaViaLockSDK", "endTrip", [accountId, bookingId, authKey]);
//   });
// };


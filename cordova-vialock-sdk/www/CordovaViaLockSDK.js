var exec = require('cordova/exec');

exports.initiate = function (accountId, success, error) {
	exec(success, error, "CordovaViaLockSDK", "initiate", [accountId]);
};

exports.unlockOutsetLock = function (accountId, bookingId, authKey, success, error) {
	exec(success, error, "CordovaViaLockSDK", "unlockOutsetLock", [accountId, bookingId, authKey]);
};

exports.unlockDestinationLock = function (accountId, bookingId, authKey, success, error) {
    exec(success, error, "CordovaViaLockSDK", "unlockDestinationLock", [accountId, bookingId, authKey]);
};
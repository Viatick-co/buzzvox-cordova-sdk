var exec = require('cordova/exec');

exports.viaLockInit = function (mac, success, error) {
	exec(success, error, "CordovaViaLockSDK", "viaLockInit", [mac]);
};

exports.viaLockOpen = function (accountId, bookingId, mac, authSecret, success, error) {
	exec(success, error, "CordovaViaLockSDK", "viaLockOpen", [accountId, bookingId, mac, authSecret]);
};

exports.viaLockDisconnect = function (mac, success, error) {
    exec(success, error, "CordovaViaLockSDK", "unlockDestinationLock", [mac]);
};

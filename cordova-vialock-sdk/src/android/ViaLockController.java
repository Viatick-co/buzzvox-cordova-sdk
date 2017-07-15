package com.viatick.cordovavialocksdk;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseArray;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ViaLockController extends CordovaPlugin {
    private static final String TAG = "HDB";

    // The following data type values are assigned by Bluetooth SIG.
    // For more details refer to Bluetooth 4.1 specification, Volume 3, Part C, Section 18.
    private static final int DATA_TYPE_FLAGS = 0x01;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07;
    private static final int DATA_TYPE_LOCAL_NAME_SHORT = 0x08;
    private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
    private static final int DATA_TYPE_TX_POWER_LEVEL = 0x0A;
    private static final int DATA_TYPE_SERVICE_DATA = 0x16;
    private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

    private Intent lockServiceIntent;
    private static RequestQueue queue;
    private static Handler mHandler;
    private static int mConnectionState = Value.STATE_DISCONNECTED;
    private boolean mConnecting = false;
    private static boolean mScanning = false;
    private static Set<BluetoothDevice> pairedDevices;
    private static CallbackContext mCallbackContext;
    private static LockDevice mLockDevice;
    private static LockDevice connectingLockDevice;
    private static ScanCallback mScanCallback;
    private static String inputMac;
    private static String mVerifyingMacAddress;
    private static HashMap<String, BluetoothDevice> connectingDevicesHashMap = new HashMap<String, BluetoothDevice>();

    public static BluetoothAdapter mAdapter = null;
    public static HashMap<String, LockDevice> deviceHashMap = new HashMap<String, LockDevice>();
    public static List<LockDevice> lockDevices = new ArrayList<LockDevice>();
    public static HashMap<String, BluetoothGatt> gattHashMap = new HashMap<String, BluetoothGatt>();
    public static HashMap<String, LockDevice> macAddressLockHashMap = new HashMap<String, LockDevice>();
    public static BluetoothManager mBluetoothManager;
    public static Activity cordovaActivity;
    public ViaLockController self = this;
    private static long masterId;
    private static String masterSecret;
    private static long buzzvoxBookingId;
    private static boolean lockUnlocked = false;
    private static boolean mIsOutsetLock = false;
    private static long popScootUserId;
    private static long popScootBookingId;
    private static String popScootAuthKey;

    // Various callback methods defined by the BLE API.
    private static final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt bGatt, int bStatus,
                                                    int bNewState) {
                    final BluetoothGatt gatt = bGatt;
                    final int status = bStatus;
                    final int newState = bNewState;
                    try {
                        String intentAction;
                        Log.i("STATUS", String.valueOf(status));
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            if (status == 0) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Thread.sleep(200);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                        BluetoothGatt mBluetoothGatt = gattHashMap.get(gatt.getDevice().getAddress());

                                        if (mBluetoothGatt != null) {
                                            mBluetoothGatt.discoverServices();
                                        } else {
                                            mCallbackContext.error("Could not connect to this lock");
                                        }
                                    }
                                });
                            }
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            connectingDevicesHashMap.remove(gatt.getDevice().getAddress());
                            Log.i("CONNECTINGDEVICESCOUNT", String.valueOf(connectingDevicesHashMap.size()));
                            Log.i("LOCKDEVICE HASHMAP SIZE", String.valueOf(deviceHashMap.size()));

                            BluetoothGatt mBluetoothGatt = gattHashMap.get(gatt.getDevice().getAddress());

                            if (status == 133) {
                                Thread.sleep(500);
                                // TRY TO RECONNECT
    //                                        mLockDevice = null;
                                Log.i("RECONNECTING", mBluetoothGatt.getDevice().getAddress());

                                connectingDevicesHashMap.put(gatt.getDevice().getAddress(), gatt.getDevice());
                                Log.i("CONNECTINGDEVICESCOUNT", String.valueOf(connectingDevicesHashMap.size()));
                                connect(mBluetoothGatt.getDevice().getAddress());
                            } else if (status == 257) {

                                if (mLockDevice != null && mLockDevice.getDevice().getAddress().equals(gatt.getDevice().getAddress())) {
    //                                            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "Could not connect to this lock");
    //                                            pluginResult.setKeepCallback(false);
    //                                            mCallbackContextInit.sendPluginResult(pluginResult);
    ////                                            mCallbackContextInit.error("Could not connect to this lock");
    //                                            deviceHashMap.put(mLockDevice.getDevice().getAddress(), mLockDevice);
    //                                            mLockDevice = null;

                                    mBluetoothGatt.disconnect();
                                    gattHashMap.remove(mBluetoothGatt.getDevice().getAddress());
                                    mBluetoothGatt.close();

    //                                            Thread.sleep(5000);
    //                                            // TRY TO RECONNECT
    //                                            connect(mLockDevice.getDevice().getAddress());
                                    mLockDevice = null;
                                } else {
                                    intentAction = Value.ActionGATT.ACTION_GATT_DISCONNECTED;
                                    mConnectionState = Value.STATE_DISCONNECTED;
                                    Log.i("HDB", "Disconnected from GATT server.");
                                    broadcastUpdate(intentAction);

                                    gattHashMap.remove(mBluetoothGatt.getDevice().getAddress());
                                    mBluetoothGatt.close();
                                    LockDevice lockDevice = deviceHashMap.get(mBluetoothGatt.getDevice().getAddress());

                                    if (mLockDevice != null && lockDevice != null && lockDevice.getDevice().getAddress().equals(mLockDevice.getDevice().getAddress())) {
                                        deviceHashMap.remove(mLockDevice.getDevice().getAddress());
                                    }

                                    synchronized (lockDevices) {
                                        Log.i("CONNECTINGLOCKDEVICE", gatt.getDevice().getAddress());
//                                        lockDevices.remove(lockDevice);

                                        if (lockDevices.size() > 0) {
                                            LockDevice lockDevice1 = lockDevices.get(lockDevices.size() - 1);
                                            lockDevices.remove(lockDevice1);
                                            Log.i("CONNECTINGLOCKDEVICE2", lockDevice1.getDevice().getAddress());

                                            Thread.sleep(200);
                                            connectingDevicesHashMap.put(gatt.getDevice().getAddress(), gatt.getDevice());
                                            Log.i("CONNECTINGDEVICESCOUNT", String.valueOf(connectingDevicesHashMap.size()));
                                            connect(lockDevice1.getDevice().getAddress());
                                        }
                                    }
                                }
                            } else {
                                /*
                                 * close() must always called when we're done with an BluetoothGatt
                                 * object otherwise we'll run out of available BluetoothGatt objects
                                 */
                                gattHashMap.remove(mBluetoothGatt.getDevice().getAddress());
                                mBluetoothGatt.close();
                            }
                        }
                    } catch (Exception e) {
                        Log.i("EXCEPTION", e.toString());
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.i("HDB", "Service discovered: " + status);
                    try {
                        if (status == BluetoothGatt.GATT_SUCCESS) {

                            String services = "";
                            for (BluetoothGattService service: gatt.getServices()) {
                                services += service.getUuid().toString() + ";";
                            }
                            Log.i("SERVICES", String.valueOf(services));

                            BluetoothGattService service = gatt.getService(
                                    UUID.fromString(Value.LockBLEUUID.PRI_SER_UUID));
//
                            // Check whether this specific service exists
                            if (service != null) {
                                Log.i("SERVICE", service.getUuid().toString());
                                BluetoothGattCharacteristic characteristic1 = service.getCharacteristic(UUID.fromString(Value.LockBLEUUID.PRI_CHA_UUID));
                                Log.i("CHARACTERISTICS1", characteristic1.toString());


                                try {
//                            if (connectingLockDevice != null) {
//                                synchronized (connectingLockDevice) {
//                                    LockDevice lockDevice = connectingLockDevice;
//                                    sendCommand(characteristic1, lockDevice.verifyRequestData());
//                                }
//                            }
                                    LockDevice lockDevice = deviceHashMap.get(gatt.getDevice().getAddress());

                                    if (lockDevice != null) {
                                        lockDevice.setCharacteristic1(characteristic1);
                                        sendCommand(gatt, characteristic1, lockDevice.verifyRequestData());
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Log.i("EXCEPTION", e.toString());
                                    mCallbackContext.error(e.toString());
                                }
                            }
//                mBluetoothGatt.close();

                        } else {
                            Log.w("HDB", "onServicesDiscovered received: " + status);
                        }
                    } catch (Exception e) {
                        Log.i("EXCEPTION", e.toString());
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    Log.i("CHARACTERISTICSREAD", ": " + status);

                    if (status == BluetoothGatt.GATT_SUCCESS) {
//                        broadcastUpdate(Value.ActionGATT.ACTION_DATA_AVAILABLE, characteristic);
                        onDataAvailable(gatt);
                    } else if (status == 133) {
//                        connect(gatt.getDevice().getAddress());
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicWrite(BluetoothGatt bgatt,
                                                  final BluetoothGattCharacteristic characteristic,
                                                  int status) {
                    Log.i("CHARACTERISTICSWRITE", ": " + status);
                    final BluetoothGatt gatt = bgatt;

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            boolean readStatus = gatt.readCharacteristic(characteristic);
                            Log.i("READ CHARACTERSTICS", String.valueOf(readStatus));
                        }
                    });
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    Log.i("CHARACTERISTIC", "Changed");
//
//                    broadcastUpdate(Value.ActionGATT.ACTION_DATA_AVAILABLE, characteristic);
                    onDataAvailable(gatt);
                }

                @Override
                // Result of a characteristic read operation
                public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                    Log.i("RELIALE WRITE COMPLETED", String.valueOf(status));
                }
            };

    // Device scan callback.
    private static BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     final byte[] scanRecord) {
                    Log.i("RSSI", String.valueOf(rssi));

                    synchronized (deviceHashMap) {
                        if (checkServiceExists(scanRecord) && !deviceHashMap.containsKey(device.getAddress())) {
                            LockDevice lockDevice;
                            if (mLockDevice != null && mLockDevice.getDevice().getAddress().equals(device.getAddress())) {
                                Log.i("RECONNECT", "RECONNECT");
                                lockDevice = mLockDevice;
                                deviceHashMap.put(device.getAddress(), lockDevice);

                                try {
//                                    Thread.sleep(Math.max((lockDevices.size() - 1) * 1000, 1000));
                                    Thread.sleep(2000);
                                    connect(device.getAddress());
//                                    mBluetoothGatt.discoverServices();
                                } catch (Exception e) {
                                    Log.i("EXCEPTION", e.toString());
                                }
                            } else {
                                lockDevice = new LockDevice(device, mHandler, device.getAddress(), device.getName());
                                deviceHashMap.put(device.getAddress(), lockDevice);
                                synchronized (lockDevices) {
                                    if (connectingDevicesHashMap.size() < 3 || (mLockDevice != null && mLockDevice.getDevice().getAddress().equals(device.getAddress()))) {
//                                        connectingLockDevice = lockDevice;
                                        connectingDevicesHashMap.put(device.getAddress(), device);
                                        Log.i("CONNECTINGDEVICESCOUNT", String.valueOf(connectingDevicesHashMap.size()));
                                        connect(device.getAddress());
                                    } else {
                                        lockDevices.add(lockDevice);
                                        Log.i("LOCKDEVICES SIZE", String.valueOf(lockDevices.size()));
                                    }
                                }
//                                try {
//                                    Thread.sleep((lockDevices.size() - 1) * 2000);
//                                    connect(device.getAddress());
//                                } catch (Exception e) {
//                                    Log.i("EXCEPTION", e.toString());
//                                }
                            }
                        }
                    }
                }
            };

    public static void onDataAvailable (BluetoothGatt mBluetoothGatt) {
        LockDevice lockDevice = deviceHashMap.get(mBluetoothGatt.getDevice().getAddress());

        if (lockDevice != null) {
            switch (lockDevice.getWriteStage()) {
                case (Value.WriteStage.VERIFY_REQUEST): {
//                            byte[] rtnData = SmartLockUtil.hexStringToHex(intent.getStringExtra(Value.ActionGATT.EXTRA_DATA));
//
//                            Log.i("EXTRA DATA", intent.getStringExtra(Value.ActionGATT.EXTRA_DATA));
                    BluetoothGattService mSVC = mBluetoothGatt.getService(UUID.fromString(Value.LockBLEUUID.PRI_SER_UUID));
                    final BluetoothGattCharacteristic mCH = mSVC.getCharacteristic(UUID.fromString(Value.LockBLEUUID.PRI_CHA_UUID));

                    byte[] rtnData = mCH.getValue();

                    try {
                        String verifiedMac = lockDevice.verifyResponse(rtnData);
                        ;
                        if (verifiedMac != null) {
                            Log.i("VERIFIEDMAC", verifiedMac);
                        }

                        Log.i("INPUTMAC", inputMac);

                        if (verifiedMac != null && verifiedMac.equals(inputMac)) {
                            sendCommand(mBluetoothGatt, mCH, lockDevice.doubleVerifyRequestData(rtnData));
                        } else {
                            disconnect(mBluetoothGatt);

                            synchronized (lockDevices) {
//                                lockDevices.remove(lockDevice);
                                if (lockDevices.size() > 0) {
                                    LockDevice lockDevice1 = lockDevices.get(lockDevices.size() - 1);
                                    lockDevices.remove(lockDevice1);

                                    Thread.sleep(200);
                                    connectingDevicesHashMap.put(lockDevice1.getDevice().getAddress(), lockDevice1.getDevice());
                                    Log.i("CONNECTINGDEVICESCOUNT", String.valueOf(connectingDevicesHashMap.size()));
                                    connect(lockDevice1.getDevice().getAddress());
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("EXCEPTION", e.toString());
                        mCallbackContext.error(Value.MessageCode.LOCK_VERIFY_FAIL);
                    }
                    break;
                }

                case (Value.WriteStage.DOUBLE_VERIFY_REQUEST): {
                    BluetoothGattService mSVC = mBluetoothGatt.getService(UUID.fromString(Value.LockBLEUUID.PRI_SER_UUID));
                    final BluetoothGattCharacteristic mCH = mSVC.getCharacteristic(UUID.fromString(Value.LockBLEUUID.PRI_CHA_UUID));

                    byte[] rtnData = mCH.getValue();

                    try {
                        if (lockDevice.doubleVerifyResponse(rtnData)) {
                            mLockDevice = lockDevice;
                            Log.i("MAC", mLockDevice.getMacAddress());

                            String mac = mLockDevice.getMacAddress();
                            if (mac != null) {
                                macAddressLockHashMap.put(mac, lockDevice);
                            }

                            if (!lockUnlocked) {
                                getLockKey(masterId, buzzvoxBookingId, mac, masterSecret, mCallbackContext, cordovaActivity);
                            } else {
                                if (!mIsOutsetLock) {
                                    endTrip(lockDevice);
                                }
                            }
                        } else {
                            mCallbackContext.error(Value.MessageCode.LOCK_VERIFY_FAIL);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        mCallbackContext.error(Value.MessageCode.LOCK_VERIFY_FAIL);
                    }
                    break;
                }

                case (Value.WriteStage.UNLOCK_REQUEST): {
                    BluetoothGattService mSVC = mBluetoothGatt.getService(UUID.fromString(Value.LockBLEUUID.PRI_SER_UUID));
                    final BluetoothGattCharacteristic mCH = mSVC.getCharacteristic(UUID.fromString(Value.LockBLEUUID.PRI_CHA_UUID));

                    byte[] rtnData = mCH.getValue();
                    Log.i("RTNDATA", Arrays.toString(rtnData));
                    Log.i("RTNDATA LENGTH", String.valueOf(rtnData.length));

                    try {
                        if (lockDevice.unlockResponse(rtnData)) {
                            int connectionState = mBluetoothManager.getConnectionState(lockDevice.getDevice(), BluetoothProfile.GATT);
                            Log.i("CONNECTIONSTATE", String.valueOf(connectionState));

//                                    if (connectionState != 2) {
//                                        synchronized (deviceHashMap) {
//                                            deviceHashMap.remove(lockDevice.getDevice().getAddress());
//                                        }
//
//                                        synchronized (lockDevices) {
//                                            lockDevices.remove(lockDevice);
//                                        }
//
//                                        mConnectionState = Value.STATE_DISCONNECTED;
//                                    } else {
//                                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, mLockDevice.getName());
//                                        pluginResult.setKeepCallback(true);
//                                        mCallbackContextInit.sendPluginResult(pluginResult);
//                                    }

                            synchronized (deviceHashMap) {
                                deviceHashMap.remove(lockDevice.getDevice().getAddress());
                            }

//                            synchronized (lockDevices) {
//                                lockDevices.remove(lockDevice);
//                            }

//                                    mConnectionState = Value.STATE_DISCONNECTED;
                            lockUnlocked = true;

                            if (mIsOutsetLock) {
                                startTrip(lockDevice);
                            } else {
                                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "Success");
                                pluginResult.setKeepCallback(true);
                                mCallbackContext.sendPluginResult(pluginResult);
                            }
                        } else {
                            mCallbackContext.error(Value.MessageCode.UNLOCK_FAIL);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("EXCEPTION", e.toString());
                        mCallbackContext.error(Value.MessageCode.UNLOCK_FAIL);
                    }
                    break;
                }
            }

//                Log.i("ACTION DATA", intent.getStringExtra(Value.ActionGATT.EXTRA_DATA));
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) throws JSONException{
        switch (requestCode) {
            case Value.REQUEST_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
                break;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void init(String mac, CallbackContext callbackContext, Activity activity) {
        cordovaActivity = activity;
        lockUnlocked = false;

        try {
            if (mLockDevice != null) {
                int state = mBluetoothManager.getConnectionState(mLockDevice.getDevice(), BluetoothProfile.GATT);
                Log.i ("MLOCKDEVICE STATE", String.valueOf(state));
                if (mLockDevice.getMacAddress().equals(mac) && state == BluetoothProfile.STATE_CONNECTED) {
                    if (!lockUnlocked) {
                        getLockKey(masterId, buzzvoxBookingId, mac, masterSecret, mCallbackContext, cordovaActivity);
                    }
                } else {
                    BluetoothGatt mBluetoothGatt = gattHashMap.get(mLockDevice.getDevice().getAddress());
                    disconnect(mBluetoothGatt);

                    mLockDevice = null;
                    lockDevices.clear();
                    deviceHashMap.clear();
                }
            } else {
                mLockDevice = null;
                lockDevices.clear();
                deviceHashMap.clear();
            }

            if (mBluetoothManager == null) {
                mBluetoothManager = (BluetoothManager) cordovaActivity.getSystemService(Context.BLUETOOTH_SERVICE);
                if (mBluetoothManager == null) {
                    Log.e(TAG, "Unable to initialize BluetoothManager.");
                    callbackContext.error("Unable to start Bluetooth scan");
                    return;
                }
            }

            if (mAdapter == null) {
                mAdapter = mBluetoothManager.getAdapter();
            }

            if (mAdapter == null) {
                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                callbackContext.error("Unable to start Bluetooth scan");
                return;
            }

            Log.i("MADAPTER", mAdapter.getAddress());

            pairedDevices = mAdapter.getBondedDevices();
            //        mHandler = handler;

            Log.i("PAIRDEVICES", Arrays.toString(pairedDevices.toArray()));

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
//                Log.i("VERSION", String.valueOf(Build.VERSION.SDK_INT));
//                ActivityCompat.requestPermissions(context,
//                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                        Value.REQUEST_COARSE_LOCATION);
//            } else {
//                discover();
//            }

            discover(mac, callbackContext);
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }
    }

    private static void getLockKey(Long userId, Long bookingId, final String mac, final String authKey, final CallbackContext callbackContext, Activity cordovaActivity) {

        HTTPRequestHelper.getKeyString(queue, cordovaActivity, new ViaInterfaces.ViaCallbackInterface() {
            @Override
            public void doWhenSuccess(JSONObject result) {
                try {
                    if (result.getInt(Key.STATUS) == Value.ViaAPI.SUCCESS) {
                        JSONObject data = result.getJSONObject(Key.DATA);
                        String keyString = data.getString(Key.KEYSTRING);
                        Log.i("KEYSTRING", keyString);

                        openLockAction(mac, keyString, callbackContext);
                    } else {
                        callbackContext.error(result.getString(Key.MESSAGE));
                    }
                } catch (Exception e) {
                    disconnectLock(mac);

                    e.printStackTrace();
                    callbackContext.error(Value.MessageCode.COULD_NOT_RETRIEVE_PASSWORD);
                }
            }

            @Override
            public void doWhenError(String error) {
                disconnectLock(mac);

                callbackContext.error(Value.MessageCode.COULD_NOT_RETRIEVE_PASSWORD);
            }
        }, userId, bookingId, mac, authKey);
    }

    private static void openLockAction (final String mac, String keyString, CallbackContext callbackContext) throws IOException {
        Log.i("MAC", mac);
        if (mLockDevice != null) {
            Log.i("LOCKDEVICE MACADDRESS", mLockDevice.getMacAddress());
            if (mac.equals(mLockDevice.getMacAddress())) {
                // Set password to unlock the device
                mLockDevice.setPassword(keyString);

                final BluetoothGattCharacteristic mCH= mLockDevice.getCharacteristic1();

//            BluetoothGattService mSVC = mBluetoothGatt.getService(UUID.fromString(Value.LockBLEUUID.PRI_SER_UUID));
//            final BluetoothGattCharacteristic mCH = mSVC.getCharacteristic(UUID.fromString(Value.LockBLEUUID.PRI_CHA_UUID));
                BluetoothGatt mBluetoothGatt = gattHashMap.get(mLockDevice.getDevice().getAddress());

                if (mBluetoothGatt == null) {
                    callbackContext.error(Value.MessageCode.LOCK_ISNT_CONNECTED);
                } else {
                    sendCommand(mBluetoothGatt, mCH, mLockDevice.unlockRequestData());
                }
            }
        } else {
            callbackContext.error(Value.MessageCode.LOCK_ISNT_CONNECTED);
        }
    }

    private static void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        cordovaActivity.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(Value.ActionGATT.EXTRA_DATA, new String(data) + "\n" +
                    stringBuilder.toString());
        }
        cordova.getActivity().sendBroadcast(intent);
    }

    public static void discover(final String mac, final CallbackContext callbackContext) {
        inputMac = mac;

        // Device scan callback.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mScanCallback =
                    new ScanCallback() {
                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
                            System.out.println("BLE// onScanResult");
                            Log.i("callbackType", String.valueOf(callbackType));
                            Log.i("result", result.toString());
                            BluetoothDevice device = result.getDevice();

                            synchronized (deviceHashMap) {
                                if (result.getScanRecord().getDeviceName() != null) {
                                    Log.i("DEVICE NAME", result.getScanRecord().getDeviceName());
                                }

                                if (!deviceHashMap.containsKey(device.getAddress())) {
                                    LockDevice lockDevice;
                                    if (mLockDevice != null && mLockDevice.getDevice().getAddress().equals(device.getAddress())) {
                                        Log.i("RECONNECT", "RECONNECT");
                                        lockDevice = mLockDevice;
                                        deviceHashMap.put(device.getAddress(), lockDevice);

                                        try {
                                            //                                    Thread.sleep(Math.max((lockDevices.size() - 1) * 1000, 1000));
//                                            Thread.sleep(2000);
                                            connectingDevicesHashMap.put(device.getAddress(), device);
                                            Log.i("CONNECTINGDEVICESCOUNT", String.valueOf(connectingDevicesHashMap.size()));

                                            connect(device.getAddress());
                                            //                                    mBluetoothGatt.discoverServices();
                                        } catch (Exception e) {
                                            Log.i("EXCEPTION", e.toString());
                                        }
                                    } else {
                                        lockDevice = new LockDevice(device, mHandler, device.getAddress(), device.getName());
                                        deviceHashMap.put(device.getAddress(), lockDevice);
                                        synchronized (lockDevices) {
//                                            if (lockDevices.size() == 1 || (mLockDevice != null && mLockDevice.getDevice().getAddress().equals(device.getAddress()))) {
                                            if (connectingDevicesHashMap.size() < 3 || (mLockDevice != null && mLockDevice.getDevice().getAddress().equals(device.getAddress()))) {
                                                //                                        connectingLockDevice = lockDevice;
                                                connectingDevicesHashMap.put(device.getAddress(), device);
                                                Log.i("CONNECTINGDEVICESCOUNT", String.valueOf(connectingDevicesHashMap.size()));
                                                connect(device.getAddress());
                                            } else {
                                                lockDevices.add(lockDevice);
                                                Log.i("LOCKDEVICES SIZE", String.valueOf(lockDevices.size()));
                                            }
                                        }
                                    }
                                }
                            }

                        }

                        @Override
                        public void onBatchScanResults(List<ScanResult> results) {
                            System.out.println("BLE// onBatchScanResults");
                        }

                        @Override
                        public void onScanFailed(int errorCode) {
                            System.out.println("BLE// onScanFailed");
                            Log.e("Scan Failed", "Error Code: " + errorCode);
                        }
                    };

            // Stop first if it is still scanning
            if (mScanning) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
//                    lockService.mAdapter = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                        }
//                    mScanning = false;
                    }
                });
            }

            // Stops scanning after a pre-defined scan period.
            try {
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
////                    lockService.mAdapter = false;
//                    mAdapter.stopLeScan(mLeScanCallback);
//                    mScanning = false;
//                }
//            }, Value.SCAN_PERIOD);
                //
                Log.i("MADAPTER", mAdapter.toString());
                final UUID bluetoothUUID = UUID.fromString(Value.LockBLEUUID.BLUETOOTH_UUID);
                final UUID uuid = UUID.fromString(Value.LockBLEUUID.PRI_SER_UUID);
                Log.i("UUID", uuid.toString());
//            mAdapter.startLeScan(new UUID[]{uuid},
//                    mLeScanCallback);

                mScanning = true;

                mHandler.post(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        try {
                            Log.i("START SCAN", "start");

                            //scan specified devices only with ScanFilter
                            ScanFilter scanFilter =
                                    new ScanFilter.Builder()
                                            .setServiceUuid(ParcelUuid.fromString(Value.LockBLEUUID.PRI_SER_UUID))
                                            .build();
                            List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
                            scanFilters.add(scanFilter);

                            ScanSettings scanSettings =
                                    new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                            .build();

                            mAdapter.getBluetoothLeScanner().startScan(scanFilters, scanSettings, mScanCallback);
                        } catch (Exception e) {
                            Log.i("EXCEPTION", e.toString());
                        }
                    }
                });
            } catch (Exception e) {
                Log.i("EXCEPTION", e.toString());
            }
        } else {
            // Stop first if it is still scanning
            if (mScanning) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
//                    lockService.mAdapter = false;
                        mAdapter.stopLeScan(mLeScanCallback);
//                    mScanning = false;
                    }
                });
            }

            // Stops scanning after a pre-defined scan period.
            try {
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
////                    lockService.mAdapter = false;
//                    mAdapter.stopLeScan(mLeScanCallback);
//                    mScanning = false;
//                }
//            }, Value.SCAN_PERIOD);
                //
                Log.i("MADAPTER", mAdapter.toString());
                final UUID bluetoothUUID = UUID.fromString(Value.LockBLEUUID.BLUETOOTH_UUID);
                final UUID uuid = UUID.fromString(Value.LockBLEUUID.PRI_SER_UUID);
                Log.i("UUID", uuid.toString());
//            mAdapter.startLeScan(new UUID[]{uuid},
//                    mLeScanCallback);

                mScanning = true;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.i("START SCAN", "start");
                            Log.i("SCAN MODE", String.valueOf(mAdapter.getScanMode()));
//                        mCallbackContextInit = callbackContext;
                            mAdapter.startLeScan(
                                    mLeScanCallback);
                            //                    mConnecting = false;
//                        mAdapter.startLeScan(new UUID[]{uuid}, mLeScanCallback);
                        } catch (Exception e) {
                            Log.i("EXCEPTION", e.toString());
                        }
                    }
                });
            } catch (Exception e) {
                Log.i("EXCEPTION", e.toString());
            }
        }

        inputMac = mac;
    }

    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData,
                                    offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            Log.e("EXCEPTION", e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }

        return uuids;
    }

    public static boolean checkServiceExists(byte[] scanRecord) {
        if (scanRecord == null) {
            return false;
        }

        int currentPos = 0;
        int advertiseFlag = -1;
        List<ParcelUuid> serviceUuids = new ArrayList<ParcelUuid>();
        String localName = null;
        int txPowerLevel = Integer.MIN_VALUE;

        SparseArray<byte[]> manufacturerData = new SparseArray<byte[]>();
        Map<ParcelUuid, byte[]> serviceData = new HashMap<ParcelUuid, byte[]>();

        try {
            while (currentPos < scanRecord.length) {
                // length is unsigned int.
                int length = scanRecord[currentPos++] & 0xFF;
                if (length == 0) {
                    break;
                }
                // Note the length includes the length of the field type itself.
                int dataLength = length - 1;
                // fieldType is unsigned int.
                int fieldType = scanRecord[currentPos++] & 0xFF;
                switch (fieldType) {
                    case DATA_TYPE_FLAGS:
                        advertiseFlag = scanRecord[currentPos] & 0xFF;
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos,
                                dataLength, 2, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_32_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_128_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_LOCAL_NAME_SHORT:
                    case DATA_TYPE_LOCAL_NAME_COMPLETE:
                        localName = new String(
                                extractBytes(scanRecord, currentPos, dataLength));
                        break;
                    case DATA_TYPE_TX_POWER_LEVEL:
                        txPowerLevel = scanRecord[currentPos];
                        break;
                    case DATA_TYPE_SERVICE_DATA:
                        // The first two bytes of the service data are service data UUID in little
                        // endian. The rest bytes are service data.
                        int serviceUuidLength = BluetoothUuid.UUID_BYTES_16_BIT;
                        byte[] serviceDataUuidBytes = extractBytes(scanRecord, currentPos,
                                serviceUuidLength);
                        ParcelUuid serviceDataUuid = BluetoothUuid.parseUuidFrom(
                                serviceDataUuidBytes);
                        byte[] serviceDataArray = extractBytes(scanRecord,
                                currentPos + serviceUuidLength, dataLength - serviceUuidLength);
                        serviceData.put(serviceDataUuid, serviceDataArray);
                        break;
                    case DATA_TYPE_MANUFACTURER_SPECIFIC_DATA:
                        // The first two bytes of the manufacturer specific data are
                        // manufacturer ids in little endian.
                        int manufacturerId = ((scanRecord[currentPos + 1] & 0xFF) << 8) +
                                (scanRecord[currentPos] & 0xFF);
                        byte[] manufacturerDataBytes = extractBytes(scanRecord, currentPos + 2,
                                dataLength - 2);
                        manufacturerData.put(manufacturerId, manufacturerDataBytes);
                        break;
                    default:
                        // Just ignore, we don't handle such data type.
                        break;
                }
                currentPos += dataLength;
            }

            if (serviceUuids.isEmpty()) {
                serviceUuids = null;
            }

//            Log.i("SERVICE UUIDS", parcelUuidToString(serviceUuids));

            for (ParcelUuid uuid: serviceUuids) {
                if (uuid.getUuid().toString().equalsIgnoreCase(Value.LockBLEUUID.PRI_SER_UUID)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // DO NOTHING
        }

        return false;
    }

    private static String parcelUuidToString(List<ParcelUuid> uuids) {
        String uuidString = "";

        for (ParcelUuid parcelUuid: uuids) {
            uuidString += parcelUuid.getUuid().toString();
        }

        return uuidString;
    }

    // Parse service UUIDs.
    private static int parseServiceUuid(byte[] scanRecord, int currentPos, int dataLength,
                                        int uuidLength, List<ParcelUuid> serviceUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(scanRecord, currentPos,
                    uuidLength);
            serviceUuids.add(BluetoothUuid.parseUuidFrom(uuidBytes));
            dataLength -= uuidLength;
            currentPos += uuidLength;
        }
        return currentPos;
    }

    // Helper method to extract bytes from byte array.
    private static byte[] extractBytes(byte[] scanRecord, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(scanRecord, start, bytes, 0, length);
        return bytes;
    }

    public static boolean connect(final String address) {
        if (mAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

//        // Previously connected device.  Try to reconnect.
//        if (mBluetoothGatt != null) {
//            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//            if (mBluetoothGatt.connect()) {
//                mConnectionState = Value.STATE_CONNECTING;
//                return true;
//            } else {
//                return false;
//            }
//        }

        final BluetoothDevice device = mAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        BluetoothGatt mBluetoothGatt = device.connectGatt(cordovaActivity, false, mGattCallback);
        gattHashMap.put(device.getAddress(), mBluetoothGatt);
        Log.d(TAG, "Trying to create a new connection.");
        mConnectionState = Value.STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public static void disconnect(BluetoothGatt mBluetoothGatt) {
        if (mAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.disconnect();
//        if (mLockDevice != null && lockDevice.getDevice().getAddress().equals(mLockDevice.getDevice().getAddress())) {
//            mLockDevice = null;
//        }

    }

    public static void sendCommand (BluetoothGatt mBluetoothGatt, BluetoothGattCharacteristic characteristic, byte[] data) throws IOException {
//        int state = mBluetoothManager.getConnectionState(, BluetoothProfile.GATT);
        if (mBluetoothGatt.getDevice() != null) {
            if (characteristic != null) {
//                Log.i("SENDCOMMAND DATA", Arrays.toString(data));
//                BluetoothGattService mSVC = mBluetoothGatt.getService(UUID.fromString(Value.LockBLEUUID.PRI_SER_UUID));
//                final BluetoothGattCharacteristic mCH = mSVC.getCharacteristic(UUID.fromString(Value.LockBLEUUID.PRI_CHA_UUID));
                characteristic.setValue(data);
                Log.i("CHARACTERISTIC", characteristic.getUuid().toString());
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                Log.i("WRITEALE", String.valueOf(SmartLockUtil.isCharacteristicWriteable(characteristic)));
                Log.i("READALE", String.valueOf(SmartLockUtil.isCharacterisitcReadable(characteristic)));
                Log.i("NOTIFIALE", String.valueOf(SmartLockUtil.isCharacterisiticNotifiable(characteristic)));

                boolean status = mBluetoothGatt.writeCharacteristic(characteristic);
                Log.i("WRITE CHARACTERSTICS", String.valueOf(status));
            } else {
                mCallbackContext.error(Value.MessageCode.COULD_NOT_CONNECT);
            }
        } else {
            mCallbackContext.error(Value.MessageCode.COULD_NOT_CONNECT);
        }
    }

    public static void disconnectLock(String macAddress) {
         /*
           * Stop Scan
           */
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (mScanCallback != null) {
                        mAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                    }
                } else {
                    mAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
            }
        });

        LockDevice lockDevice = macAddressLockHashMap.get(macAddress);

        if (lockDevice != null) {
            disconnect(gattHashMap.get(lockDevice.getDevice().getAddress()));
        }
    }

    public static void initialize (Activity cordovaActivity) {
        queue = Volley.newRequestQueue(cordovaActivity);
        mHandler = new Handler(Looper.getMainLooper());

        // Register for GATT action
        IntentFilter filter = new IntentFilter(Value.ActionGATT.ACTION_GATT_CONNECTED);
        filter.addAction(Value.ActionGATT.ACTION_GATT_DISCONNECTED);
        filter.addAction(Value.ActionGATT.ACTION_DATA_AVAILABLE);
        filter.addAction(Value.ActionGATT.ACTION_GATT_SERVICES_DISCOVERED);
    }

    public static void startTripInitCheck(final long accountId, final long bookingId, final String authKey, final CallbackContext callbackContext, final Activity activity) {
        if (masterSecret == null) {
            ActivityCompat.requestPermissions(cordovaActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    Value.REQUEST_COARSE_LOCATION);

            HTTPRequestHelper.appInit(queue, new ViaInterfaces.ViaCallbackInterface() {
                @Override
                public void doWhenSuccess(JSONObject result) {
                    try {
                        masterId = result.getLong(Key.MASTERID);
                        masterSecret = result.getString(Key.MASTERSECRET);

                        startTrip(accountId,bookingId,authKey,callbackContext,activity);
                    } catch (Exception e) {
                        e.printStackTrace();
                        callbackContext.error(e.getMessage());
                    }
                }

                @Override
                public void doWhenError(String error) {
                    callbackContext.error(error);
                }
            }, accountId);
        } else {
            startTrip(accountId,bookingId,authKey,callbackContext,activity);
        }
    }

    public static void startTrip(final long accountId, final long bookingId, final String authKey, final CallbackContext callbackContext, final Activity activity) {
        Log.i("STARTTRIP", "called");
        cordovaActivity = activity;

        HTTPRequestHelper.getBookingInfo(queue, new ViaInterfaces.ViaCallbackInterface() {
            @Override
            public void doWhenSuccess(JSONObject result) {
                try {
                    long lockId = result.getLong(Key.OUTSETLOCKID);
                    String integrateId = result.getString(Key.INTEGRATEID);
                    buzzvoxBookingId = Long.parseLong(integrateId.substring(13, integrateId.length()));
                    Log.i("buzzvoxBookingId", String.valueOf(buzzvoxBookingId));

                    HTTPRequestHelper.getLockInfo(queue, new ViaInterfaces.ViaCallbackInterface() {
                        @Override
                        public void doWhenSuccess(JSONObject result) {
                            try {
                                if (result.getInt(Key.STATUS) == Value.ViaAPI.SUCCESS) {
                                    JSONObject data = result.getJSONObject(Key.DATA);
                                    final String macAddress = data.getString(Key.MAC);

                                    LockDevice lockDevice = macAddressLockHashMap.get(macAddress);
                                    if (lockDevice != null) {
                                        final BluetoothGatt gatt = gattHashMap.get(lockDevice.getDevice().getAddress());
                                        if (gatt != null) {
                                            int connectionState = mBluetoothManager.getConnectionState(lockDevice.getDevice(), BluetoothProfile.GATT);
                                            Log.i("CONNECTIONSTATE", String.valueOf(connectionState));

                                            if (connectionState == 2) {
                                                // Lock is connected means that user has closed the lock. Now we can proceed to start trip
                                                HTTPRequestHelper.startTrip(queue, new ViaInterfaces.ViaCallbackInterface() {
                                                    @Override
                                                    public void doWhenSuccess(JSONObject result) {
                                                        try {
                                                            if (result.getInt(Key.STATUS) == Value.ViaAPI.SUCCESS) {
                                                                JSONObject data = result.getJSONObject(Key.DATA);
                                                                callbackContext.success(data.getString(Key.TRIP_ID));

                                                                disconnectLock(macAddress);
                                                            } else {
                                                                callbackContext.error(Value.MessageCode.COULD_NOT_START_TRIP);
                                                            }
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                            callbackContext.error(Value.MessageCode.COULD_NOT_START_TRIP);
                                                        }
                                                    }

                                                    @Override
                                                    public void doWhenError(String error) {
                                                        callbackContext.error(Value.MessageCode.COULD_NOT_START_TRIP);
                                                    }
                                                }, accountId, bookingId, authKey);

                                            } else {
                                                // Lock isn't in the state "connected"
                                                callbackContext.error(Value.MessageCode.NOT_CLOSED);
                                            }
                                        } else {
                                            // No gatt of the corresponding mac address means the lock isn't connected or not anymore
                                            callbackContext.error(Value.MessageCode.NOT_CLOSED);
                                        }
                                    } else {
                                        // Lock device not in the macAddressLockHashMap anymore means that the lock isn't connected or not anymore
                                        callbackContext.error(Value.MessageCode.NOT_CLOSED);
                                    }
                                } else {
                                    callbackContext.error(result.getString(Key.MESSAGE));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                callbackContext.error(e.getMessage());
                            }
                        }

                        @Override
                        public void doWhenError(String error) {
                            callbackContext.error(error);
                        }
                    }, lockId, masterSecret);
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }

            @Override
            public void doWhenError(String error) {
                callbackContext.error(error);
            }
        }, accountId, bookingId, authKey);
    }

    /*
     * Start trip automatically after unlock the outset lock
     */
    public static void startTrip (final LockDevice lockDevice) {
        disconnectLock(lockDevice.getMacAddress());

        HTTPRequestHelper.startTrip(queue, new ViaInterfaces.ViaCallbackInterface() {
            @Override
            public void doWhenSuccess(JSONObject result) {
                try {
                    mCallbackContext.success("Success");
                    disconnectLock(lockDevice.getMacAddress());
                } catch (Exception e) {
                    e.printStackTrace();
                    mCallbackContext.error(Value.MessageCode.COULD_NOT_START_TRIP);
                }
            }

            @Override
            public void doWhenError(String error) {
                mCallbackContext.error(Value.MessageCode.COULD_NOT_START_TRIP);
            }
        }, popScootUserId, popScootBookingId, popScootAuthKey);
    }

    /*
     * End trip automatically after unlock the destination lock
     */
    public static void endTrip (final LockDevice lockDevice) {
        if (lockDevice != null) {
            disconnectLock(lockDevice.getMacAddress());

            HTTPRequestHelper.endTrip(queue, new ViaInterfaces.ViaCallbackInterface() {
                @Override
                public void doWhenSuccess(JSONObject result) {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "TRIP_ENDED");
                    pluginResult.setKeepCallback(true);
                    mCallbackContext.sendPluginResult(pluginResult);
                    return;
                }

                @Override
                public void doWhenError(String error) {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, Value.MessageCode.COULD_NOT_END_TRIP);
                    pluginResult.setKeepCallback(false);
                    mCallbackContext.sendPluginResult(pluginResult);
                    return;
                }
            }, popScootUserId, popScootBookingId, popScootAuthKey);
        } else {
            // Lock device not in the macAddressLockHashMap anymore means that the lock isn't connected or not anymore
            mCallbackContext.error(Value.MessageCode.NOT_CLOSED);
            return;
        }
    }

    public static void endTripInitCheck(final long accountId, final long bookingId, final String authKey, final CallbackContext callbackContext, final Activity activity) {
        if (masterSecret == null) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    Value.REQUEST_COARSE_LOCATION);

            HTTPRequestHelper.appInit(queue, new ViaInterfaces.ViaCallbackInterface() {
                @Override
                public void doWhenSuccess(JSONObject result) {
                    try {
                        masterId = result.getLong(Key.MASTERID);
                        masterSecret = result.getString(Key.MASTERSECRET);

                        endTrip(accountId,bookingId,authKey,callbackContext,activity);
                    } catch (Exception e) {
                        e.printStackTrace();
                        callbackContext.error(e.getMessage());
                    }
                }

                @Override
                public void doWhenError(String error) {
                    callbackContext.error(error);
                }
            }, accountId);
        } else {
            endTrip(accountId,bookingId,authKey,callbackContext,activity);
        }
    }

    public static void endTrip(final long accountId, final long bookingId, final String authKey, final CallbackContext callbackContext, final Activity activity) {
        cordovaActivity = activity;

        HTTPRequestHelper.getBookingInfo(queue, new ViaInterfaces.ViaCallbackInterface() {
            @Override
            public void doWhenSuccess(JSONObject result) {
                try {
                    long lockId = result.getLong(Key.DESTINATIONLOCKID);
                    String integrateId = result.getString(Key.INTEGRATEID);
                    buzzvoxBookingId = Long.parseLong(integrateId.substring(13, integrateId.length()));
                    Log.i("buzzvoxBookingId", String.valueOf(buzzvoxBookingId));

                    HTTPRequestHelper.getLockInfo(queue, new ViaInterfaces.ViaCallbackInterface() {
                        @Override
                        public void doWhenSuccess(JSONObject result) {
                            try {
                                if (result.getInt(Key.STATUS) == Value.ViaAPI.SUCCESS) {
                                    JSONObject data = result.getJSONObject(Key.DATA);
                                    final String macAddress = data.getString(Key.MAC);

                                    LockDevice lockDevice = macAddressLockHashMap.get(macAddress);
                                    if (lockDevice != null) {
                                        final BluetoothGatt gatt = gattHashMap.get(lockDevice.getDevice().getAddress());

                                        if (gatt != null) {
                                            int connectionState = mBluetoothManager.getConnectionState(lockDevice.getDevice(), BluetoothProfile.GATT);

                                            if (connectionState == 2) {
                                                // Lock is connected means that user has closed the lock. Now we can proceed to start trip
                                                HTTPRequestHelper.endTrip(queue, new ViaInterfaces.ViaCallbackInterface() {
                                                    @Override
                                                    public void doWhenSuccess(JSONObject result) {
                                                        try {
                                                            if (result.getInt(Key.STATUS) == Value.ViaAPI.SUCCESS) {
                                                                JSONObject data = result.getJSONObject(Key.DATA);
                                                                callbackContext.success(data.getString(Key.TRIP_ID));

                                                                disconnectLock(macAddress);
                                                            } else {
                                                                callbackContext.error(Value.MessageCode.COULD_NOT_END_TRIP);
                                                            }
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                            callbackContext.error(Value.MessageCode.COULD_NOT_END_TRIP);
                                                        }
                                                    }

                                                    @Override
                                                    public void doWhenError(String error) {
                                                        callbackContext.error(Value.MessageCode.COULD_NOT_END_TRIP);
                                                    }
                                                }, accountId, bookingId, authKey);
                                            } else {
                                                // Lock isn't in the state "connected"
                                                callbackContext.error(Value.MessageCode.NOT_CLOSED);
                                            }
                                        } else {
                                            // No gatt of the corresponding mac address means the lock isn't connected or not anymore
                                            callbackContext.error(Value.MessageCode.NOT_CLOSED);
                                        }
                                    } else {
                                        // Lock device not in the macAddressLockHashMap anymore means that the lock isn't connected or not anymore
                                        callbackContext.error(Value.MessageCode.NOT_CLOSED);
                                    }
                                } else {
                                    callbackContext.error(result.getString(Key.MESSAGE));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                callbackContext.error(e.getMessage());
                            }
                        }

                        @Override
                        public void doWhenError(String error) {
                            callbackContext.error(error);
                        }
                    }, lockId, masterSecret);
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }

            @Override
            public void doWhenError(String error) {
                callbackContext.error(error);
            }
        }, accountId, bookingId, authKey);
    }

    public static void initiate(Long accountId, final CallbackContext callbackContext, Activity activity) {
        cordovaActivity = activity;

        ActivityCompat.requestPermissions(cordovaActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                Value.REQUEST_COARSE_LOCATION);

        HTTPRequestHelper.appInit(queue, new ViaInterfaces.ViaCallbackInterface() {
            @Override
            public void doWhenSuccess(JSONObject result) {
                try {
                    masterId = result.getLong(Key.MASTERID);
                    masterSecret = result.getString(Key.MASTERSECRET);
                    callbackContext.success("success");
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }

            @Override
            public void doWhenError(String error) {
                callbackContext.error(error);
            }
        }, accountId);
    }

    public static void unlockLockInitCheck(final long accountId, final long bookingId, final String authKey, final CallbackContext callbackContext, final Activity activity, final boolean isOutsetLock) {
        mCallbackContext = callbackContext;

        if (masterSecret == null) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    Value.REQUEST_COARSE_LOCATION);

            HTTPRequestHelper.appInit(queue, new ViaInterfaces.ViaCallbackInterface() {
                @Override
                public void doWhenSuccess(JSONObject result) {
                    try {
                        masterId = result.getLong(Key.MASTERID);
                        masterSecret = result.getString(Key.MASTERSECRET);

                        unlockLock(accountId,bookingId,authKey,callbackContext,activity,isOutsetLock);
                    } catch (Exception e) {
                        e.printStackTrace();
                        callbackContext.error(e.getMessage());
                    }
                }

                @Override
                public void doWhenError(String error) {
                    callbackContext.error(error);
                }
            }, accountId);
        } else {
            unlockLock(accountId,bookingId,authKey,callbackContext,activity,isOutsetLock);
        }
    }

    public static void unlockLock(long accountId, long bookingId, String authKey, final CallbackContext callbackContext, final Activity activity, final boolean isOutsetLock) {
        mIsOutsetLock = isOutsetLock;
        popScootUserId = accountId;
        popScootBookingId = bookingId;
        popScootAuthKey = authKey;

        HTTPRequestHelper.getBookingInfo(queue, new ViaInterfaces.ViaCallbackInterface() {
            @Override
            public void doWhenSuccess(JSONObject result) {
                try {
                    long lockId = 0;
                    if (isOutsetLock) {
                        lockId = result.getLong(Key.OUTSETLOCKID);
                    } else {
                        lockId = result.getLong(Key.DESTINATIONLOCKID);
                    }

                    String integrateId = result.getString(Key.INTEGRATEID);
                    buzzvoxBookingId = Long.parseLong(integrateId.substring(13, integrateId.length()));
                    Log.i("buzzvoxBookingId", String.valueOf(buzzvoxBookingId));

                    HTTPRequestHelper.getLockInfo(queue, new ViaInterfaces.ViaCallbackInterface() {
                        @Override
                        public void doWhenSuccess(JSONObject result) {
                            try {
                                if (result.getInt(Key.STATUS) == Value.ViaAPI.SUCCESS) {
                                    JSONObject data = result.getJSONObject(Key.DATA);
                                    String mac = data.getString(Key.MAC);
                                    init(mac,callbackContext,activity);
                                } else {
                                    callbackContext.error(result.getString(Key.MESSAGE));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                callbackContext.error(e.getMessage());
                            }
                        }

                        @Override
                        public void doWhenError(String error) {
                            callbackContext.error(error);
                        }
                    },lockId,masterSecret);
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }

            @Override
            public void doWhenError(String error) {
                callbackContext.error(error);
            }
        }, accountId, bookingId, authKey);
    }
}

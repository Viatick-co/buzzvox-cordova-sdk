package com.viatick.cordovavialocksdk;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LockController {

    private static final int OPT_FLAG = 0;
    private static final String TAG = LockController.class.getSimpleName();
    Activity activity = null;

    public ArrayList<LockDevice> LOCK_DEVICES = new ArrayList<LockDevice>();
    private BluetoothLeScanner bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

    private String inputMac;
    private String inputPassword;
    private CallbackContext viaLocInitCallbackContext;
    private CallbackContext viaLocOpenCallbackContext;
    private LockService mLockService;
    private LockDevice mLockDevice;

    protected ScanCallback scanCallBack = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            processDevice(result.getDevice());
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    protected LockCallback lockCallback = new LockCallback() {
        @Override
        public void onConnect(LockDevice lockDevice) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(true);
            viaLocInitCallbackContext.sendPluginResult(pluginResult);
        }

        @Override
        public void onOpen(LockDevice lockDevice) {
            viaLocOpenCallbackContext.success(lockDevice.getMac());
            int idx = indexOf(LOCK_DEVICES, mLockDevice.getBluetoothDevice().getAddress());
            LOCK_DEVICES.remove(idx);
        }

        @Override
        public void onDisconnect(LockDevice lockDevice) {
            Log.i(TAG, "onDisconnect: " + lockDevice.toString());
            switch (OPT_FLAG) {
                case 0:
                    // DO NOTHING...
                    break;
                case 1:
                    for(LockDevice ld: LOCK_DEVICES) {
                        if(ld.getConnectIndex() == 0) {
                            Log.i("CALLED", "FLAG 2");
                            connectDevice(ld);
                            break;
                        }
                    }
                    break;
            }
        }
    };

    public boolean requestPermission(Activity activity, int permissionIdx) {
        this.activity = activity;
        if(ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, permissionIdx);

            return false;
        } else {
            return true;
        }
    }

    public void connectLock(String mac, CallbackContext callbackContext) {
        viaLocInitCallbackContext = callbackContext;
        this.inputMac = mac;
        scanLocks();
    }

    public void openLock(String mac, String password, CallbackContext callbackContext) {
        viaLocOpenCallbackContext = callbackContext;
        if (this.inputMac.equals(mac)) {
            this.inputPassword = password;
            openDevice();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void scanLocks() {
        LOCK_DEVICES.clear();
        ParcelUuid parcelUuid = ParcelUuid.fromString(LockDevice.PRISERUUID);
        ScanFilter.Builder sfBuilder = new ScanFilter.Builder();
        sfBuilder.setServiceUuid(parcelUuid);

        List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
        scanFilters.add(sfBuilder.build());

        ScanSettings.Builder ssBuilder = new ScanSettings.Builder();
        bluetoothLeScanner.startScan(scanFilters, ssBuilder.build(), scanCallBack);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void stopScan() {
        bluetoothLeScanner.stopScan(scanCallBack);
    }

    protected int indexOf(ArrayList<LockDevice> lockDevices, String address) {
        int idx = -1;
        for(int i = 0; i < lockDevices.size(); i ++) {
            LockDevice ld = lockDevices.get(i);
            if(ld.getBluetoothDevice().getAddress().equals(address)) {
                return i;
            }
        }
        return idx;
    }

    protected void processDevice(BluetoothDevice bluetoothDevice) {
        // Log.i(TAG, bluetoothDevice.getAddress());
        if(bluetoothDevice.getAddress() != null) {
            int idx = indexOf(LOCK_DEVICES, bluetoothDevice.getAddress());
            if (idx == -1) {
                LockDevice lockDevice = new LockDevice(bluetoothDevice, bluetoothDevice.getName());
                LOCK_DEVICES.add(lockDevice);
                Log.i(TAG, "LOCK_DEVICES: " + LOCK_DEVICES.toString());
                // First Option
                switch (OPT_FLAG) {
                    case 0:
                        if(this.matchMac(inputMac, bluetoothDevice.getAddress())) {
                            Log.i("CALLED", "FLAG 1");
                            connectDevice(lockDevice);
                        }
                        break;
                    case 1:
                        int zeroIndex = 0;
                        for(LockDevice ld: LOCK_DEVICES) {
                            if(ld.getConnectIndex() == 0) {
                                zeroIndex += 1;
                            }
                        }
                        if(zeroIndex == 1) {
                            Log.i("CALLED", "FLAG 1");
                            connectDevice(lockDevice);
                        }
                        break;
                }
            }
        }
    }

    protected boolean matchMac(String mac, String address) {
        // process address
        String[] data = address.split(":");
        String seedMac_1 = "";
        String seedMac_2 = "";
        for(String s: data) {
            seedMac_1 = seedMac_1 + s;
            seedMac_2 = s + seedMac_2;
        }
        if(seedMac_1.toLowerCase().equals(mac) || seedMac_2.toLowerCase().equals(mac)) {
            return true;
        }
        return false;
    }

    protected void connectDevice(LockDevice lockDevice) {
        mLockDevice = lockDevice;
        mLockService = new LockService();
        mLockService.inputMac = this.inputMac;
        mLockService.lockCallback = lockCallback;
        mLockService.connectDevice(activity, lockDevice);
    }

    private void openDevice () {
        if (mLockService != null && mLockDevice != null) {
            try {
                mLockService.inputPassword = this.inputPassword;
                mLockService.openDevice(activity, mLockDevice);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        if (mLockService != null) {
            mLockService.disconnectDevice();
        }
    }
}

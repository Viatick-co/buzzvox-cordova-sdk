package com.viatick.cordovavialocksdk;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by yaqing.bie on 11/12/17.
 */

public class LockDevice {
    public static final String PRISERUUID = "0000FFE0-0000-1000-8000-00805F9B34FB";
    public static final String PRICHAUUID = "0000FFF1-0000-1000-8000-00805F9B34FB";

    public static final String VERIFY_PASSWORD = "741689";
    public static final String ORIMDATA = "oriMData";
    public static final String ENCYMDATA = "encyMData";

    private BluetoothDevice bluetoothDevice;

    private String name;
    private String password;
    private String mac;
    private boolean connected;
    private int connectIndex;

    public LockDevice(BluetoothDevice bluetoothDevice, String name) {
        this.bluetoothDevice = bluetoothDevice;
        this.name = name;
        this.connected = false;
        this.connectIndex = 0;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getConnectIndex() {
        return connectIndex;
    }

    public void setConnectIndex(int connectIndex) {
        this.connectIndex = connectIndex;
    }

    public String toString() {
        String bluetoothDeviceString = null;
        if(this.bluetoothDevice != null) {
            bluetoothDeviceString = this.bluetoothDevice.getAddress();
        }
        return "[" +
                this.name + ", " +
                this.password + ", " +
                this.mac + ", " +
                this.connected + ", " +
                bluetoothDeviceString + ", " +
                this.connectIndex +
                "]";
    }
}

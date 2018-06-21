package com.viatick.cordovavialocksdk;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by yaqing.bie on 12/12/17.
 */

public class LockService {

    public LockCallback lockCallback;

    private final static String TAG = LockService.class.getSimpleName();

    public BluetoothGatt bluetoothGatt;
    public LockDevice lockDevice;

    public String inputMac;
    public String inputPassword;

    protected int flag = 0;

    LockUtil lockUtil = new LockUtil();

    protected BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
//        @Override
//        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
//            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
//        }
//
//        @Override
//        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
//            super.onPhyRead(gatt, txPhy, rxPhy, status);
//        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            BluetoothDevice bluetoothDevice = gatt.getDevice();
            Log.i(TAG, bluetoothDevice.getAddress());
            Log.i(TAG, bluetoothDevice.getName());
            switch(newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "STATE_CONNECTED");
                    lockDevice.setConnected(true);
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    lockDevice.setConnected(false);
                    Log.i(TAG, "STATE_DISCONNECTED");
                    break;
                default:
                    Log.i(TAG, "Status " + Integer.toString(status));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            switch(status) {
                case BluetoothGatt.GATT_SUCCESS:
                    Log.i(TAG, "GATT_SUCCESS");
                    for (BluetoothGattService bgs: gatt.getServices()) {
                        UUID bgsUUID = bgs.getUuid();
                        String s = bgsUUID.toString().toUpperCase();
                        if (s.equals(LockDevice.PRISERUUID)) {
                            Log.i(TAG, "PRISERUUID");
                            BluetoothGattCharacteristic characteristic = bgs.getCharacteristic(UUID.fromString(LockDevice.PRICHAUUID));
                            try {
                                characteristic.setValue(lockUtil.verifyRequestData());
                                gatt.writeCharacteristic(characteristic);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Log.i(TAG, lockDevice.toString());

                        } else {
                            Log.i(TAG, "uuid " + bgsUUID.toString().toUpperCase());
                        }
                    }
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    Log.i(TAG, "GATT_FAILURE");
                    break;
                default:
                    Log.i(TAG, "Status " + Integer.toString(status));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.i(TAG, "onCharacteristicRead");
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    String s = characteristic.getUuid().toString().toUpperCase();
                    if (s.equals(LockDevice.PRICHAUUID)) {
                        Log.i(TAG, "PRICHAUUID");
                        byte[] rtnData = characteristic.getValue();
                        try {
                            switch (flag) {
                                case 0:
                                    String mac = lockUtil.verifyResponse(rtnData);
                                    if (mac != null) {
                                        lockDevice.setMac(mac);
                                        characteristic.setValue(lockUtil.doubleVerifyRequestData(rtnData));
                                        gatt.writeCharacteristic(characteristic);
                                        flag = 1;
                                    } else {
                                        disconnectDevice();
                                    }
                                    break;
                                case 1:
                                    if (lockUtil.doubleVerifyResponse(rtnData)) {
                                        Log.i(TAG, "Connected!");
                                        /**
                                         * if the input mac is equals to the lock mac then try open the lock
                                         * else disconnect after get mac address and connect successful
                                         * */
                                        if (inputMac != null && lockDevice.getMac().equals(inputMac)) {
//                                                characteristic.setValue(lockUtil.unlockRequestData(inputPassword));
//                                                gatt.writeCharacteristic(characteristic);
                                            lockCallback.onConnect(lockDevice);
                                            flag = 2;
                                        } else {
                                            disconnectDevice();
                                        }
                                    } else {
                                        disconnectDevice();
                                    }
                                    break;
                                case 2:
                                    if (lockUtil.unlockResponse(rtnData)) {
                                        Log.i(TAG, "success unlock the lock");
                                        lockCallback.onOpen(lockDevice);
//                                        disconnectDevice();
                                    } else {
                                        disconnectDevice();
                                    }
                                    break;
                                default:
                                    disconnectDevice();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    Log.i(TAG, "GATT_FAILURE");
                    break;
                default:
                    Log.i(TAG, "Status " + Integer.toString(status));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG, "onCharacteristicWrite");
            switch(status) {
                case BluetoothGatt.GATT_SUCCESS:
                    String s = characteristic.getUuid().toString().toUpperCase();
                    if (s.equals(LockDevice.PRICHAUUID)) {
                        Log.i(TAG, "PRICHAUUID");
                        gatt.readCharacteristic(characteristic);

                    } else {
                        Log.i(TAG, "uuid " + characteristic.getUuid().toString().toUpperCase());
                    }
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    Log.i(TAG, "GATT_FAILURE");
                    break;
                default:
                    Log.i(TAG, "Status " + Integer.toString(status));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG, "onCharacteristicChanged");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    public void connectDevice(Activity activity, LockDevice lockDevice) {
        this.lockDevice = lockDevice;
        this.flag = 0;
        this.bluetoothGatt = this.lockDevice.getBluetoothDevice().connectGatt(activity, false, bluetoothGattCallback);
    }

    public void openDevice(Activity activity, LockDevice lockDevice) throws IOException {
        BluetoothGattCharacteristic characteristic = this.bluetoothGatt.getService(UUID.fromString(LockDevice.PRISERUUID))
                .getCharacteristic(UUID.fromString(LockDevice.PRICHAUUID));
        characteristic.setValue(lockUtil.unlockRequestData(inputPassword));
        this.bluetoothGatt.writeCharacteristic(characteristic);
    }

    public void disconnectDevice() {
        this.bluetoothGatt.disconnect();
        this.lockDevice.setConnectIndex(this.lockDevice.getConnectIndex() + 1);
        this.lockCallback.onDisconnect(this.lockDevice);
    }
}

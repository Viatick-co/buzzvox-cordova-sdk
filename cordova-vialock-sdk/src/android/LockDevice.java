package com.viatick.cordovavialocksdk;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class LockDevice {
    private Handler handler;
    private BluetoothGattCharacteristic characteristic1;
//    private BluetoothGatt gatt;
    private BluetoothDevice device;
    private long id;
    private String name;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String password;
    private String newPassword;
    private String macAddress;
    private String identifier;
    private boolean vibration;
    private boolean verified;
    private boolean connected;
    private long battery;
    private int writeStage;
    private static final String TAG = "MY_APP_DEBUG_TAG";

    public LockDevice(BluetoothDevice device, Handler handler, String identifier, String name) {
//        this.gatt = gatt;
        this.device = device;
        this.identifier = identifier;
        this.name = name;
        this.handler = handler;
    }

//    public void unlock (ViaBatteryController mContext) throws IOException {
//        unlockRequest(mContext);
//    }

    public byte[] unlockRequestData () throws IOException {
        byte[] CONSTANT_1 = new byte[] {(byte) 0xa1};
//        byte[] password = this.password.getBytes();
        byte[] password = this.password.getBytes();
        byte[] CONSTANT_2 = new byte[] {0x01};

        byte[] data = SmartLockUtil.mergeByteArray(CONSTANT_1, password, CONSTANT_2);
        writeStage = Value.WriteStage.UNLOCK_REQUEST;

        Log.i("UNLOCKREQUEST", Arrays.toString(data));
//        mContext.sendCommand(characteristic1,data);
        return data;
    }

    private void changePassword () {
        if (this.newPassword.toCharArray().length == 6) {
            changePasswordRequest();
        } else {
            Log.i("MSG", "Password has to be 6 char long...");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void changePasswordRequest () {
        byte[] rtnData = this.characteristic1.getValue();
        short[] commandByte = SmartLockUtil.byteArrToshortArr(rtnData);

        if (rtnData.length == 3 && commandByte[0] == 0xa2 && commandByte[1] == 0x07 &&
                commandByte[2] == 0x00) {
            this.password = this.newPassword;
            Log.i("MSG", "Password changed...");
        } else {
            this.connected = false;
        }
    }

//    private void resetPassword () throws IOException {
//        resetPasswordRequest();
//    }
//
//    private void resetPasswordRequest() throws IOException {
//        byte[] data = new byte[] {(byte) 0xa1, 0x31, 0x36, 0x38, 0x31, 0x36, 0x38, 0x08, 0x31, 0x36, 0x38, 0x31, 0x36, 0x38};
//        this.writeStage = Value.WriteStage.RESET_PASSWORD_REQUEST;
//
//        sendCommand(data);
//    }
//
//    private void resetPasswordResponse () {
//        Log.i("MSG", "Password resetted...");
//    }
//

    HashMap<String, byte[]> CRC16DIC = new HashMap<String, byte[]>();
    public byte[] verifyRequestData () throws IOException {
        byte[] CONSTANT_1 = new byte[] {(byte) 0xa1};
        byte[] password = Value.VERIFY_PASSWORD.getBytes();
        byte[] CONSTANT_2 = new byte[] {0x05};

        byte[] mData = SmartLockUtil.mergeByteArray(CONSTANT_1, password, CONSTANT_2);
        Log.i("MDATA1", Arrays.toString(mData));

        CRC16DIC = SmartLockUtil.get11Digits();
        byte[] ori11MData = CRC16DIC.get(Key.ORIMDATA);
        Log.i("ORI11MDATA", Arrays.toString(ori11MData));
        mData = SmartLockUtil.mergeByteArray(mData, ori11MData);
        writeStage = Value.WriteStage.VERIFY_REQUEST;

        Log.i("MDATA2", Arrays.toString(mData));

//        mContext.sendCommand(this,mData);

        return mData;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public String verifyResponse (byte[] rtnData) throws IOException {

        Log.i("RTNDATA", Arrays.toString(rtnData));
        Log.i("RTNDATA LENGTH", String.valueOf(rtnData.length));
        Log.i("RTNDATA0", Integer.toString((int) rtnData[0]));
//        Log.i("COMMANDBYTE", Arrays.toString(commandByte));
//        Log.i("COMMANDBYTE LENGTH", String.valueOf(commandByte.length));
        if (rtnData.length == 19 && rtnData[0] == (byte) 0xa2 && rtnData[1] == (byte) 0x05 && rtnData[2] == (byte) 0x00) {
            byte[] macAddressData = Arrays.copyOfRange(rtnData, 3, 9);
            setMacAddress(SmartLockUtil.bytesToHex(macAddressData));

            byte[] subData = Arrays.copyOfRange(rtnData, 9, 19);

            Log.i("SUDATA", Arrays.toString(subData));
            Log.i("ENCYMDATA", Arrays.toString(CRC16DIC.get(Key.ENCYMDATA)));

            if (Arrays.equals(subData, CRC16DIC.get(Key.ENCYMDATA))) {
//                doubleVerifyRequest(rtnData, mContext);
                return macAddress;
            }
        }

        return null;
    }

    public byte[] doubleVerifyRequestData (byte[] rtnData) throws IOException {
        Log.i("WRITE STAGE", "doubleVerify");
        byte[] CONSTANT_1 = new byte[] {(byte) 0xa1};
        byte[] password = Value.VERIFY_PASSWORD.getBytes();
        byte[] CONSTANT_2 = new byte[] {0x09};

        byte[] mData = SmartLockUtil.mergeByteArray(CONSTANT_1, password, CONSTANT_2);

        HashMap<String, byte[]> dic = SmartLockUtil.get12Digits(rtnData);
        byte[] ency12MData = dic.get(Key.ENCYMDATA);
        mData = SmartLockUtil.mergeByteArray(mData, ency12MData);
        writeStage = Value.WriteStage.DOUBLE_VERIFY_REQUEST;

        return mData;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean doubleVerifyResponse (byte[] rtnData) throws IOException {

        Log.i("RTNDATA", Arrays.toString(rtnData));
        Log.i("RTNDATA LENGTH", String.valueOf(rtnData.length));
        Log.i("RTNDATA0", Integer.toString((int) rtnData[0]));
//        Log.i("COMMANDBYTE", Arrays.toString(commandByte));
//        Log.i("COMMANDBYTE LENGTH", String.valueOf(commandByte.length));
        if (rtnData.length == 3 && rtnData[0] == (byte) 0xa2 && rtnData[1] == (byte) 0x09 && rtnData[2] == (byte) 0x00) {
            return true;
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean unlockResponse (byte[] rtnData) throws IOException {

        Log.i("RTNDATA", Arrays.toString(rtnData));
        Log.i("RTNDATA LENGTH", String.valueOf(rtnData.length));
        Log.i("RTNDATA0", Integer.toString((int) rtnData[0]));
//        Log.i("COMMANDBYTE", Arrays.toString(commandByte));
//        Log.i("COMMANDBYTE LENGTH", String.valueOf(commandByte.length));
        if (rtnData.length == 3 && rtnData[0] == (byte) 0xa2 && rtnData[1] == (byte) 0x01 && rtnData[2] == (byte) 0x00) {
            return true;
        }

        return false;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getWriteStage() {
        return writeStage;
    }

    public void setWriteStage(int writeStage) {
        this.writeStage = writeStage;
    }

    public long getBattery() {
        return battery;
    }

    public void setBattery(long battery) {
        this.battery = battery;
    }

    public void setCharacteristic1(BluetoothGattCharacteristic characteristic1) {
        this.characteristic1 = characteristic1;
    }

    public BluetoothGattCharacteristic getCharacteristic1() {
        return characteristic1;
    }
}

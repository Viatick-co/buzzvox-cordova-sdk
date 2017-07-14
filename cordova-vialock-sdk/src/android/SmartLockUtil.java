package com.viatick.cordovavialocksdk;


import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SmartLockUtil {
    private static final List<String> hexList = new ArrayList<String>(Arrays.asList("0", "1", "2", "3", "4" ,"5", "6", "7", "8", "9",
            "a", "b", "c", "d", "e", "f"));
//    private static final List<String> posiList = new ArrayList<String>(Arrays.asList("2", "3", "4" ,"5", "6", "7", "8", "9", "a"));

    public static byte[] mergeByteArray (byte[]... byteArrays) {
        int totalLength = 0;
        for (byte[] byteArr: byteArrays) {
            totalLength += byteArr.length;
        }

        byte[] merged = new byte[totalLength];

        int currentPointer = 0;
        for (byte[] byteArr: byteArrays) {
            System.arraycopy(byteArr,0,merged,currentPointer,byteArr.length);
            currentPointer += byteArr.length;
        }

        return merged;
    }

    public static short[] byteArrToshortArr (byte[] bytes) {
        short[] shorts = new short[bytes.length/2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

        return shorts;
    }

    public static byte[] shortArrToByteArr (short[] buffer) {
        ByteBuffer byteBuf = ByteBuffer.allocate(2 * buffer.length);
        for (short s: buffer) {
            byteBuf.putShort(s);
        }

        return byteBuf.array();
    }

    public static byte[] toBytes(int i)
    {
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);

        return result;
    }

    public static HashMap<String, byte[]> get11Digits () {
        // 4 position
        List<String> dyPosiList = new ArrayList<String>(Arrays.asList("2", "3", "4" ,"5", "6", "7", "8", "9", "a"));
        List<String> posiArr = new ArrayList<String>();

        for (int i = 0;i <= 3;i++) {
            int rdmIdx = (int) (Math.random() * dyPosiList.size());

            String val = dyPosiList.get(rdmIdx);
            dyPosiList.remove(rdmIdx);
            posiArr.add(val);
        }
        Log.i("GET11DIGITS POSIARR", Arrays.toString(posiArr.toArray()));

        // 18 digits
        List<String> digitArr = new ArrayList<String>();

        for (int i = 0;i <= 17;i++) {
            int rdmIdx = (int) (Math.random() * hexList.size());
            digitArr.add(hexList.get(rdmIdx));
        }
        Log.i("GET11DIGITS DIGITARR", Arrays.toString(digitArr.toArray()));

        byte[] oriMData = new byte[0];
        for (int i : new ArrayList<Integer>(Arrays.asList(0,2))) {
            String str = posiArr.get(i) +  posiArr.get(i+1);
            oriMData = mergeByteArray(oriMData, new byte[] {hexStringToHex(str)[0]});
        }
        Log.i("GET11DIGITS ORIMDATA 1", Arrays.toString(oriMData));

        for (int i : new ArrayList<Integer>(Arrays.asList(0,2,4,6,8,10,12,14,16))) {
            String str = digitArr.get(i) +  digitArr.get(i+1);
            oriMData = mergeByteArray(oriMData, new byte[] {hexStringToHex(str)[0]});
        }
        Log.i("GET11DIGITS ORIMDATA 2", Arrays.toString(oriMData));

        List<String> seedArr = new ArrayList<String>();
        List<Integer> digiPosiIdxArr = new ArrayList<Integer>(Arrays.asList(0,1,2,3,4,5,6,7,8));
        for (String i: posiArr) {
            int idx = Integer.parseInt(i, 16);
            digiPosiIdxArr.remove(digiPosiIdxArr.indexOf(idx - 2));
            seedArr.add(digitArr.get((idx - 2) * 2) + digitArr.get((idx - 2) * 2 + 1));
        }
        Log.i("SEEDARR", Arrays.toString(seedArr.toArray()));

        // get calculat number
        List<String> calNumArr = new ArrayList<String>();
        for (int i: digiPosiIdxArr) {
            calNumArr.add(digitArr.get(i * 2) + digitArr.get(i * 2 + 1));
        }
        Log.i("CALNUMARR", Arrays.toString(calNumArr.toArray()));

        // crc16 calculation
        byte[] seedDigiMData = new byte[0];
        for (String i: seedArr) {
            byte[] hex = hexStringToHex(i);
            Log.i("HEX", Arrays.toString(hex));
            seedDigiMData = mergeByteArray(seedDigiMData, new byte[] {hex[0]});
        }
        Log.i("SEEDIGIMDATA", Arrays.toString(seedDigiMData));

        byte[] encyMData = new byte[0];
        for (String i: calNumArr) {
            byte[] crcDigiData = new byte[0];
            crcDigiData = mergeByteArray(crcDigiData, new byte[] {hexStringToHex(i)[0]});
            crcDigiData = mergeByteArray(crcDigiData, seedDigiMData);
            Log.i("CRCDIGIDATA", Arrays.toString(crcDigiData));

            byte[] crc16DigiData = crc16(crcDigiData);
//            byte[] crc16DigiData = calculate_crc(crcDigiData);

            Log.i("CRC16DIGIDATA", Arrays.toString(crc16DigiData));
            encyMData = mergeByteArray(encyMData, crc16DigiData);
        }
        Log.i("ENCYMDATA", Arrays.toString(encyMData));

        HashMap<String, byte[]> rtnDic = new HashMap<String, byte[]>();
        rtnDic.put(Key.ORIMDATA, oriMData);
        rtnDic.put(Key.ENCYMDATA, encyMData);

        return rtnDic;
    }

    public static HashMap<String, byte[]> get12Digits (byte[] dataArr) {
        // 4 position
        byte[] byteOri = new byte[] {dataArr[3], dataArr[4], dataArr[5], dataArr[6], dataArr[7],
                dataArr[8], dataArr[10], dataArr[12], dataArr[14], dataArr[16], dataArr[18]};

        byte[] oriMData = new byte[0];
        oriMData = mergeByteArray(oriMData, byteOri);

        byte[] encyMData = new byte[0];

        for (int i = 0;i < 5;i++) {
            byte[] crcDigiData = new byte[] {dataArr[i + 3], dataArr[10], dataArr[12], dataArr[14],
                    dataArr[16], dataArr[18]};

            // CRC16
            byte[] crc16DigiData = crc16(crcDigiData);
            encyMData = mergeByteArray(encyMData, crc16DigiData);
        }

        HashMap<String, byte[]> rtnDic = new HashMap<String, byte[]>();
        rtnDic.put(Key.ORIMDATA, oriMData);
        rtnDic.put(Key.ENCYMDATA, encyMData);

        return rtnDic;
    }

    public static byte[] hexStringToHex(String hex){
        byte[] hexByteArr = new byte[]{};

        for(int i=0; i< hex.length()-1; i+=2 ){
            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            hexByteArr = mergeByteArray(hexByteArr, new byte[]{(byte) decimal});
        }

        return hexByteArr;
    }

    public static byte[] crc16(byte[] bytes) {
        int[] table = {
                0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241,
                0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440,
                0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40,
                0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841,
                0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40,
                0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41,
                0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641,
                0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
                0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
                0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441,
                0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41,
                0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840,
                0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41,
                0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40,
                0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640,
                0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
                0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240,
                0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441,
                0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41,
                0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840,
                0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
                0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
                0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640,
                0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041,
                0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241,
                0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440,
                0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40,
                0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841,
                0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40,
                0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41,
                0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641,
                0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040,
        };

        int crc = 0x0000;
        for (byte b : bytes) {
            crc = (crc >>> 8) ^ table[(crc ^ b) & 0xff];
        }

        byte[] crcByteArr = BigInteger.valueOf(crc).toByteArray();
//        return Integer.toHexString(crc).getBytes();
        if (crcByteArr.length >= 2) {
            return new byte[] {crcByteArr[crcByteArr.length - 2], crcByteArr[crcByteArr.length - 1]};
        } else {
            return new byte[] {0x0000, crcByteArr[crcByteArr.length - 1]};
        }
    }

    /**
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWriteable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * @return Returns <b>true</b> if property is Readable
     */
    public static boolean isCharacterisitcReadable(BluetoothGattCharacteristic pChar) {
        return ((pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    /**
     * @return Returns <b>true</b> if property is supports notification
     */
    public static boolean isCharacterisiticNotifiable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}

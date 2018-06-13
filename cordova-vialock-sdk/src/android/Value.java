package com.viatick.cordovavialocksdk;


public class Value {
    public static final int STATE_DISCONNECTED = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int REQUEST_COARSE_LOCATION = 1;
    public static final long SCAN_PERIOD = 10000;

    public static class WriteStage {
        public static final int VIBRATE_REQUEST = 0;
        public static final int UNLOCK_REQUEST = 1;
        public static final int RESET_PASSWORD_REQUEST = 2;
        public static final int VERIFY_REQUEST = 3;
        public static final int DOUBLE_VERIFY_REQUEST = 4;
    }

    public static class Message {
        public static final int MESSAGE_STATE_CHANGE = 1;
        public static final int MESSAGE_READ = 2;
        public static final int MESSAGE_WRITE = 3;
        public static final int MESSAGE_DEVICE_NAME = 4;
        public static final int MESSAGE_TOAST = 5;
    }

    public static class ActionGATT {
        public final static String ACTION_GATT_CONNECTED =
                "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
        public final static String ACTION_GATT_DISCONNECTED =
                "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
        public final static String ACTION_GATT_SERVICES_DISCOVERED =
                "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
        public final static String ACTION_DATA_AVAILABLE =
                "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
        public final static String EXTRA_DATA =
                "com.example.bluetooth.le.EXTRA_DATA";
    }

    public static class LockBLEUUID {
        public final static String PRI_SER_UUID = "0000FFE0-0000-1000-8000-00805F9B34FB";
        public final static String PRI_CHA_UUID = "0000FFF1-0000-1000-8000-00805F9B34FB";
        public final static String BLUETOOTH_UUID = "00001800-0000-1000-8000-00805F9B34FB";
    }

    public static final String VERIFY_PASSWORD = "741689";
    public static final String END_POINT = "https://buzzvox.co";
    public static final String GET = "GET";

    public class ViaAPI {
        public static final long SUCCESS = 1L;
    }
}

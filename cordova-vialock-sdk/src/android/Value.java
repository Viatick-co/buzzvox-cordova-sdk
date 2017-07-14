package com.viatick.cordovavialocksdk;

public class Value {
    public static final int STATE_DISCONNECTED = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int REQUEST_COARSE_LOCATION = 1;
    public static final long SCAN_PERIOD = 10000;

    public static class MessageCode {
        public static final String NOT_CLOSED = "The lock is not closed yet";
        public static final String COULD_NOT_RETRIEVE_PASSWORD = "Could not retrieve lock password";
        public static final String COULD_NOT_START_TRIP = "Could not start trip";
        public static final String COULD_NOT_END_TRIP = "Could not end trip";
        public static final String LOCK_VERIFY_FAIL = "Lock verification failed";
        public static final String UNLOCK_FAIL = "Failed to unlock";
        public static final String LOCK_ISNT_CONNECTED = "Lock isn't connected";
        public static final String COULD_NOT_CONNECT = "Could not connect to the lock";
    }

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

    public static final String BUZZVOX_API_DOMAIN = "https://buzzvox.co";
    public static final String POPSCOOT_API_DOMAIN = "http://test.popscoot.com/popscoot/api";
    public static final String GET = "GET";

    public class ViaAPI {
        public static final long SUCCESS = 1L;
    }
}

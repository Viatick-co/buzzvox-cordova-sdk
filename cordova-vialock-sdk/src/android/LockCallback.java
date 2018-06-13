package com.viatick.cordovavialocksdk;

/**
 * Created by yaqing.bie on 13/12/17.
 */

interface LockCallback {
    void onConnect(LockDevice lockDevice);
    void onOpen(LockDevice lockDevice);
    void onDisconnect(LockDevice lockDevice);
}

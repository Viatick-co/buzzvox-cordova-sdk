package com.viatick.cordovavialocksdk;

import android.annotation.TargetApi;
import android.content.IntentFilter;
import android.os.Build;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ViaLockController extends CordovaPlugin {
    private static final String TAG = "HDB";
    private RequestQueue queue;
    private LockController lockController = new LockController();

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        queue = Volley.newRequestQueue(cordova.getActivity());

        // Register for GATT action
        IntentFilter filter = new IntentFilter(Value.ActionGATT.ACTION_GATT_CONNECTED);
        filter.addAction(Value.ActionGATT.ACTION_GATT_DISCONNECTED);
        filter.addAction(Value.ActionGATT.ACTION_DATA_AVAILABLE);
        filter.addAction(Value.ActionGATT.ACTION_GATT_SERVICES_DISCOVERED);
    };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("viaLockInit")) {
            lockController.connectLock(args.getString(0), callbackContext);
            return true;
        } else if (action.equals("viaLockOpen")) {
            getLockKey(args.getLong(0), args.getLong(1), args.getString(2), callbackContext, args.getString(3), lockController);
            return true;
        } else if (action.equals("viaLockDisconnect")) {
            lockController.disconnect();
            lockController.stopScan();
            callbackContext.success();
            return true;
        } else {
            return true;
        }
    };

    private void getLockKey(Long userId, Long bookingId, final String mac, final CallbackContext callbackContext, final String authKey, final LockController lockController) {
        HTTPRequestHelper.getKeyString(queue, cordova.getActivity(), new ViaInterfaces.ViaCallbackInterface() {
            @Override
            public void doWhenSuccess(JSONObject result) {
                try {
                    if (result.getInt(Key.STATUS) == Value.ViaAPI.SUCCESS) {
                        JSONObject data = result.getJSONObject(Key.DATA);
                        String keyString = data.getString(Key.KEYSTRING);

                        lockController.openLock(mac, keyString, callbackContext);
                    } else {
                        callbackContext.error(result.getString(Key.MESSAGE));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.toString());
                }
            }

            @Override
            public void doWhenError(String error) {
                callbackContext.error(error);
            }
        }, userId, bookingId, mac, authKey);
    }
}

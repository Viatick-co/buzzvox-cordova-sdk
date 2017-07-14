package com.viatick.cordovavialocksdk;

import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.toolbox.Volley;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class CordovaViaLockSDK extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      Log.i("action", action);

      if (action.equals("initiate")) {
          ViaLockController.initiate(args.getLong(0), callbackContext, cordova.getActivity());
          return true;
      } else if (action.equals("unlockOutsetLock")) {
          ViaLockController.unlockLockInitCheck(args.getLong(0), args.getLong(1), args.getString(2), callbackContext, cordova.getActivity(), true);
          return true;
      } else if (action.equals("unlockDestinationLock")) {
          ViaLockController.unlockLockInitCheck(args.getLong(0), args.getLong(1), args.getString(2), callbackContext, cordova.getActivity(), false);
          return true;
      } else if (action.equals("startTrip")) {
          ViaLockController.startTripInitCheck(args.getLong(0), args.getLong(1), args.getString(2), callbackContext, cordova.getActivity());
          return true;
      } else if (action.equals("endTrip")) {
          ViaLockController.endTripInitCheck(args.getLong(0), args.getLong(1), args.getString(2), callbackContext, cordova.getActivity());
          return true;
      }

      return false;
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        ViaLockController.initialize(cordova.getActivity());
    };
}

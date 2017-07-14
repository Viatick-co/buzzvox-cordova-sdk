package com.viatick.cordovavialocksdk;

import org.json.JSONObject;

public class ViaInterfaces {
    //define callback interface
    public interface ViaCallbackInterface {
        void doWhenSuccess(JSONObject result);
        void doWhenError(String error);
    }
}

package com.viatick.cordovavialocksdk;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HTTPRequestHelper {
    public static void getKeyString (RequestQueue queue,
                                      Context context,
                                      final ViaInterfaces.ViaCallbackInterface callbackInterface,
                                      long userId, long bookingId, String mac, final String authKey) {

        String url = Value.END_POINT + "/blesys/service/collect/lockkey/" + userId + "/" + bookingId +
                "/" + mac + "?_=" + Math.random();
        Log.i("URL", url);
        // Request a string response from the provided URL.
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url
                , new JSONObject(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.println(Log.INFO,"JSON_RESPONSE",response.toString());

                            callbackInterface.doWhenSuccess(response);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                callbackInterface.doWhenError(error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                params.put("auth_secret", authKey);
                return params;
            }
        };

        /*
         * Retry (max. 2 times) in case of timeout
         */
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,2,1));
        // Add the request to the RequestQueue.
        queue.add(request);
    }

    public static void recordAction (RequestQueue queue) throws JSONException {
        JSONObject input = new JSONObject();
        input.put("accountId", 354);
        input.put("action", "open app");
        input.put("device", "Redmi Note 4");
        input.put("latitude", 1.2831859);
        input.put("longitude", 103.8486842);
        input.put("os", "android");
        input.put("remark", "");
        input.put("version", "1.0.9");

        String url = Value.END_POINT + "/blesys/service/res/actions";
        Log.i("URL", url);
        // Request a string response from the provided URL.
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url
                , input,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.println(Log.INFO,"JSON_RESPONSE",response.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                params.put("auth_secret", "_d548bi7hjfjv2ppur3naahssqdtijirmc5m070bfa7dku4tad2m");
                return params;
            }
        };

        /*
         * Retry (max. 2 times) in case of timeout
         */
//        request.setRetryPolicy(new DefaultRetryPolicy(
//                10000,2,1));
        // Add the request to the RequestQueue.
        queue.add(request);
    }
}

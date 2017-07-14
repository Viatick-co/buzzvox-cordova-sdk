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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HTTPRequestHelper {
    public static void getKeyString (RequestQueue queue,
                                     Context context,
                                     final ViaInterfaces.ViaCallbackInterface callbackInterface,
                                     long userId, long bookingId, String mac, final String authKey) {

        String url = Value.BUZZVOX_API_DOMAIN + "/blesys/service/collect/lockkey/" + userId + "/" + bookingId +
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

    public static void startTrip (RequestQueue queue,
                                  final ViaInterfaces.ViaCallbackInterface callbackInterface,
                                  long accountId, long bookingId, final String authKey) {

        String url = Value.POPSCOOT_API_DOMAIN + "/trip" +
                "?_=" + Math.random();
        Log.i("URL", url);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("accountId", accountId);
            jsonObject.put("bookingId", bookingId);
            jsonObject.put("action", "start");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Request a string response from the provided URL.
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url
                , jsonObject,
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
                params.put("Auth-Secret", authKey);
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

    public static void endTrip (RequestQueue queue,
                                  final ViaInterfaces.ViaCallbackInterface callbackInterface,
                                  long accountId, long bookingId, final String authKey) {

        String url = Value.POPSCOOT_API_DOMAIN + "/trip" +
                "?_=" + Math.random();
        Log.i("URL", url);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("accountId", accountId);
            jsonObject.put("bookingId", bookingId);
            jsonObject.put("action", "end");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Request a string response from the provided URL.
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url
                , jsonObject,
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
                params.put("Auth-Secret", authKey);
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

    public static void appInit(RequestQueue queue, final ViaInterfaces.ViaCallbackInterface callbackInterface, Long accountId) {
        String url = Value.POPSCOOT_API_DOMAIN + "/extra/init" +
                "?_=" + Math.random();
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

    public static void getBookingInfo (RequestQueue queue,
                                final ViaInterfaces.ViaCallbackInterface callbackInterface,
                                long accountId, long bookingId, final String authKey) {

        String url = Value.POPSCOOT_API_DOMAIN + "/accounts/" + accountId + "/bookings/" + bookingId +
                "?_=" + Math.random();
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
                params.put("Auth-Secret", authKey);
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

    public static void getLockInfo (RequestQueue queue,
                                       final ViaInterfaces.ViaCallbackInterface callbackInterface,
                                       long lockId, final String masterSecret) {
        String url = Value.BUZZVOX_API_DOMAIN + "/blesys/service/res/lite-bicycle-locks/" + lockId +
                "?_=" + Math.random();
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
                params.put("auth_secret", masterSecret);
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
}

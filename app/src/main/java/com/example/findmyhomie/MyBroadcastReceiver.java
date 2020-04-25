package com.example.findmyhomie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MyBroadcastReceiver extends BroadcastReceiver {
    static final class BroadcastTypes {
        static final String SPOTIFY_PACKAGE = "com.spotify.music";
        static final String PLAYBACK_STATE_CHANGED = SPOTIFY_PACKAGE + ".playbackstatechanged";
        static final String QUEUE_CHANGED = SPOTIFY_PACKAGE + ".queuechanged";
        static final String METADATA_CHANGED = SPOTIFY_PACKAGE + ".metadatachanged";
    }

    private String spotifySongID = "";
    private static String ACCESSTOKEN = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        // This is sent with all broadcasts, regardless of type. The value is taken from
        // System.currentTimeMillis(), which you can compare to in order to determine how
        // old the event is.
        long timeSentInMs = intent.getLongExtra("timeSent", 0L);
//action.equals(BroadcastTypes.METADATA_CHANGED)
        String action = intent.getAction();

        if (action.equals("com.spotify.music.metadatachanged")) {
            String trackId = intent.getStringExtra("id");
            String artistName = intent.getStringExtra("artist");
            String albumName = intent.getStringExtra("album");
            String trackName = intent.getStringExtra("track");
            int trackLengthInSec = intent.getIntExtra("length", 0);
            spotifySongID = trackId.split(":")[2];

            // Do something with extracted information...
//            GetAccessToken(context, spotifySongID);
            UpdateMyData(context, spotifySongID);
        }
    }

    private void UpdateMyData(Context _context, String _trackID) {
        Log.d("BroadCastReceiver", "Song Changed");
        UserRepository userRepository = new UserRepository(_context);
        User myUser = userRepository.getUser("HSadeghein");
        myUser.setSpotifySongID(_trackID);
        userRepository.updateTask(myUser);
        GetAccessToken(_context, _trackID);

    }
    private void GetAccessToken(Context _context, String _trackID) {


        JSONObject jsonBodyObj = new JSONObject();
        try {
            jsonBodyObj.put("grant_type", "client_credentials");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String requestBody = "grant_type=client_credentials";
        RequestQueue queue = Volley.newRequestQueue(_context);
        String url = "https://accounts.spotify.com/api/token";
// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responseJson = new JSONObject(response);
                            ACCESSTOKEN = responseJson.get("access_token").toString();
                            GetTrackInfo(_context, _trackID);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String body = null;
                try {
                    body = new String(error.networkResponse.data,"UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.d("GetAccessToken", "Request was not sent => Error: " + body);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Basic M2RlMTFjOGE1NTFmNDdjMWFiYzJmYzgxM2IzOWY3Mzc6YzExMzM3ODE2ZjY2NDQyMjg4Nzc4Njc4MWJiNzZmOWI=");

                return params;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {


                try {
                    return requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=utf-8";
            }
        };

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void GetTrackInfo(Context _context, String _trackID) {
        try {
            SpotifySongData spotifySongData = new SpotifySongData();
            RequestQueue queue = Volley.newRequestQueue(_context);
            String url = "https://api.spotify.com/v1/tracks/" + _trackID;
            Log.d("UpdateMyData", ACCESSTOKEN);

            // Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Display the first 500 characters of the response string.
                            Log.d("GetTrackInfo", "Request sent");
                            try {
                                JSONObject obj = new JSONObject(response);
                                spotifySongData.trackID = _trackID;
                                spotifySongData.name = obj.getString("name");
                                spotifySongData.Album = obj.getJSONObject("album").getString("name");
                                spotifySongData.Artist = obj.getJSONArray("artists").getJSONObject(0).getString("name");
                                spotifySongData.url = obj.getJSONObject("external_urls").getString("spotify");
                                spotifySongData.imgURL = obj.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url");
                                spotifySongData.uri = obj.getString("uri");
                                UserRepository userRepository = new UserRepository(_context);
                                spotifySongData.username = userRepository.getAllUsers().get(0).username;
                                if(MySingelton.getInstance().markerHashMap.containsKey(spotifySongData.username)) {
                                    MySingelton.getInstance().markerHashMap.get(spotifySongData.username).setTag(spotifySongData);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String body = null;
                    try {
                        body = new String(error.networkResponse.data,"UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    Log.d("GetTrackInfo", "Request was not sent => Error: " + body);
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Accept", "application/json");
                    params.put("Content-Type", "application/json");
                    params.put("Authorization", "Bearer " + ACCESSTOKEN);

                    return params;
                }
            };

            // Add the request to the RequestQueue.
            queue.add(stringRequest);


        } catch (SQLiteConstraintException e) {
            Log.d("GetTrackInfo", e.getMessage());
        }

    }


}
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
            GetAccessToken(context, spotifySongID);
        }
    }

    private void UpdateMyData(Context _context, String _trackID) {
        try {
            UserRepository userRepository = new UserRepository(_context);
            User myUser = userRepository.getUser("HSadeghein");
            myUser.setSpotifySongID(_trackID);
            userRepository.updateTask(myUser);
//            userRepository.insertUser("Mohammad Hassan Sadeghein","HSadeghein",65.048599f,25.471414f,"MySpotifyUsername");
//        User myUser = userRepository.getUser("HSadeghein");
//        System.out.println(myUser.getFullName());
//        System.out.println(myUser.getLat());
//            String spotifySongID = "";
//            List<User> users = userRepository.getAllUsers();
//            for (User u : users) {
//                Log.d("MainActivity", u.username);
//                spotifySongID = u.spotifySongID;
//                Log.d("MainActivity", spotifySongID);
//            }

            RequestQueue queue = Volley.newRequestQueue(_context);
            String url = "https://api.spotify.com/v1/tracks/" + _trackID;
            Log.d("UpdateMyData", ACCESSTOKEN);

// Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Display the first 500 characters of the response string.
                            Log.d("UpdateMyData", "Request sent");
                            Log.d("UpdateMyData", response);

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("UpdateMyData", "Request was not sent => Error: " + error.getMessage());
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
            Log.d("UpdateMyData", e.getMessage());
        }


//        mMap.addMarker(new MarkerOptions().position(new LatLng(65.048599f, 25.471414f)).title("Friend1"));
//        mMap.addMarker(new MarkerOptions().position(new LatLng(myUser.getLat(), myUser.getLng())).title(myUser.spotifyUsername));
//
//        mMap.addMarker(new MarkerOptions().position(new LatLng(65.078599f, 25.071414f)).title("Friend2"));
//        mMap.addMarker(new MarkerOptions().position(new LatLng(63.048599f, 25.471414f)).title("Friend3"));
//        mMap.addMarker(new MarkerOptions().position(new LatLng(65.148599f, 24.471414f)).title("Friend4"));

    }

    private void GetAccessToken(Context _context, String _trackID) {


        JSONObject jsonBodyObj = new JSONObject();
        try{
            jsonBodyObj.put("grant_type", "client_credentials");
        }catch (JSONException e){
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
                            UpdateMyData(_context, _trackID);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("GetAccessToken", "Request was not sent => Error: " + error.getMessage());
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
}
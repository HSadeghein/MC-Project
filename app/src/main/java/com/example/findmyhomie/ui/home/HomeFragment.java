package com.example.findmyhomie.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteConstraintException;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.findmyhomie.MySingelton;
import com.example.findmyhomie.PopActivity;
import com.example.findmyhomie.R;
import com.example.findmyhomie.SpotifySongData;
import com.example.findmyhomie.User;
import com.example.findmyhomie.UserRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class HomeFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    RemoteMongoCollection _remoteCollection;

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private Button btn;
    private FusedLocationProviderClient mFusedLocationClient;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;


    private String ACCESSTOKEN = "";


    public HomeFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            mapFragment = SupportMapFragment.newInstance();
            ft.replace(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());


        //MongoDB Atlas and Stitch
        final StitchAppClient client = Stitch.initializeAppClient("findmyhomie-ryoth");

        // Log-in using an Anonymous authentication provider from Stitch
        client.getAuth().loginWithCredential(new AnonymousCredential())
                .addOnCompleteListener(new OnCompleteListener<StitchUser>() {
                    @Override
                    public void onComplete(@NonNull Task<StitchUser> task) {
                        // Get a remote client
                        final RemoteMongoClient remoteMongoClient =
                                client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");

                        // Set up the atlas collection
                        _remoteCollection = remoteMongoClient
                                .getDatabase("Android").getCollection("users");
                    }
                });


        return root;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener) this);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120000); // two minute interval
        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                //Place current location marker
                UserRepository userRepository = new UserRepository(getContext());
                Log.d("Database", "the size is " + userRepository.getAllUsers().size());
                User myUser = userRepository.getAllUsers().get(0);
                String trackID = myUser.spotifySongID;
                GetAccessToken(getContext(), trackID);
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(    myUser.username);
                mCurrLocationMarker = mMap.addMarker(markerOptions);
                MySingelton.getInstance().markerHashMap.put(myUser.username, mCurrLocationMarker);
                //move map camera
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));


            }
        }
    };
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(getContext())
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(getContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getContext(), "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public boolean onMarkerClick(final Marker marker) {
        SpotifySongData songData = (SpotifySongData) marker.getTag();
        System.out.println("Marker Listener");
        Intent i = new Intent(getActivity(), PopActivity.class);
        i.putExtra("SpotifySongData", songData);
        Activity activity = (Activity) getContext();
        activity.startActivity(i);
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    private void addSapleMarker() {
        UserRepository userRepository = new UserRepository(getContext());
        userRepository.insertUser("Mohammad Hassan Sadeghein", "HSadeghein", 65.048599f, 25.471414f, "MySpotifyUsername");
        User myUser = userRepository.getUser("HSadeghein");
        System.out.println(myUser.getFullName());
        System.out.println(myUser.getLat());

//        mMap.addMarker(new MarkerOptions().position(new LatLng(65.048599f, 25.471414f)).title("Friend1"));
//        mMap.addMarker(new MarkerOptions().position(new LatLng(myUser.getLat(), myUser.getLng())).title(myUser.spotifyUsername));
//
        mMap.addMarker(new MarkerOptions().position(new LatLng(65.078599f, 25.071414f)).title("Friend2"));
        mMap.addMarker(new MarkerOptions().position(new LatLng(63.048599f, 25.471414f)).title("Friend3"));
        mMap.addMarker(new MarkerOptions().position(new LatLng(65.148599f, 24.471414f)).title("Friend4"));

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
                            GetTrackInfo(getContext(), _trackID);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String body = null;
                try {
                    body = new String(error.networkResponse.data, "UTF-8");
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
                                spotifySongData.username = mCurrLocationMarker.getTitle();
                                mCurrLocationMarker.setTag(spotifySongData);

                                Log.d("GetTrackInfo", spotifySongData.imgURL);
                                Log.d("GetTrackInfo", spotifySongData.uri);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String body = null;
                    try {
                        body = new String(error.networkResponse.data, "UTF-8");
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



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
import android.os.Handler;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
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
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.client.model.Filters;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateOptions;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;

import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class HomeFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    RemoteMongoCollection<Document> _remoteCollection;

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 30 * 1000; //Delay for 15 seconds.  One second = 1000 milliseconds.

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private Button btn;
    private FusedLocationProviderClient mFusedLocationClient;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;


    private String ACCESSTOKEN = "";


    @Override
    public void onStart() {
        super.onStart();
        //User existed in database

        UserRepository userRepository = new UserRepository(getContext());
        int size = userRepository.getAllUsers().size();
        Log.d("Database", "size" + size);
        if (size == 0) {
            Log.d("XDXDXD", "size is " + size);
//            Intent i = new Intent(getActivity(), Login.class);
//            Activity activity = (Activity) getContext();
//            startActivityForResult(i, 0);
            byte[] array = new byte[10]; // length is bounded by 7
            new Random().nextBytes(array);
            String generatedString = new String(array, Charset.forName("UTF-8"));
            User user = new User();
            user.setUsername(generatedString);
            userRepository.insertUser(user);
            Log.d("XDXDXD", "size is " + userRepository.getAllUsers().size());

        }


        //MongoDB Atlas and Stitch
        if (!Stitch.hasAppClient("findmyhomie-etrmr")) {
            final StitchAppClient client =
                    Stitch.initializeDefaultAppClient("findmyhomie-etrmr");

            final RemoteMongoClient mongoClient =
                    client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");

            final RemoteMongoCollection<Document> _remoteCollection =
                    mongoClient.getDatabase("Android").getCollection("users");


        }

    }


    @Override
    public void onResume() {
        //start handler as activity become visible

        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                updateHashMap();
                PushMyDataToMongo();
//                RenderMarkers();
                handler.postDelayed(runnable, delay);
            }
        }, delay);

        super.onResume();
    }

// If onPause() is not included the threads will double up when you
// reload the activity

    @Override
    public void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("ReturnActivity", "OK");
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                UserRepository userRepository = new UserRepository(getContext());
                User result = (User) data.getSerializableExtra("result");
                result.setLat((float) mLastLocation.getLatitude());
                result.setLng((float) mLastLocation.getLongitude());
                userRepository.updateTask(result);
                LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(result.username);
                mCurrLocationMarker = mMap.addMarker(markerOptions);
                MySingelton.getInstance().markerHashMap.put(result.username, mCurrLocationMarker);
                //move map camera
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }//onActivityResult

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


        return root;
    }

    @Override
    public void onConnected(Bundle bundle) {



    }

    @Override
    public void onConnectionSuspended(int i) {

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

        updateHashMap();
        PushMyDataToMongo();
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
                if (userRepository.getAllUsers().size() != 0) {
                    User myUser = userRepository.getAllUsers().get(0);

                    String trackID = myUser.spotifySongID;
                    if (trackID != "")
                        GetAccessToken(getContext(), trackID, myUser.getUsername());
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    myUser.setLat((float) latLng.latitude);
                    myUser.setLng((float) latLng.longitude);
                    userRepository.updateTask(myUser);
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title(myUser.username);
                    mCurrLocationMarker = mMap.addMarker(markerOptions);
                    MySingelton.getInstance().markerHashMap.put(myUser.username, mCurrLocationMarker);
                    //move map camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                }


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

    private void GetAccessToken(Context _context, String _trackID, String _username) {


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
                            GetTrackInfo(getContext(), _trackID, _username);
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

    private void GetTrackInfo(Context _context, String _trackID, String _username) {
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
                                spotifySongData.username = _username;
                                Marker marker = MySingelton.getInstance().markerHashMap.get(_username);
                                marker.setTag(spotifySongData);

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

    private void updateHashMap() {
        final StitchAppClient client =
                Stitch.getAppClient("findmyhomie-etrmr");

        final RemoteMongoClient mongoClient =
                client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");

        final RemoteMongoCollection<Document> _remoteCollection =
                mongoClient.getDatabase("Android").getCollection("users");

        client.getAuth().loginWithCredential(new AnonymousCredential()).continueWithTask(new Continuation<StitchUser, Task<List<Document>>>() {
            @Override
            public Task<List<Document>> then(@NonNull Task<StitchUser> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.e("STITCH", "Login Failed!");
                    throw task.getException();
                }
                List<Document> docs = new ArrayList<>();

                return _remoteCollection
                        .find()
                        .into(docs);
            }
        }).addOnCompleteListener(new OnCompleteListener<List<Document>>() {
            @Override
            public void onComplete(@NonNull Task<List<Document>> task) {
                if (task.isSuccessful()) {
                    UserRepository userRepository = new UserRepository(getContext());
                    if (userRepository.getAllUsers().size() == 1) {
                        String myUsername = userRepository.getAllUsers().get(0).username;
                        for (Document doc : task.getResult()) {
                            if (!doc.getString("username").trim().equals(myUsername.trim()) && doc.getString("username") != null) {
                                String trackID = doc.getString("trackID");
                                String username = doc.getString("username");
                                Log.d("STITCH", "trackID : " + trackID);

                                if (MySingelton.getInstance().markerHashMap.containsKey(username)) {
                                    Marker marker = MySingelton.getInstance().markerHashMap.get(username);
                                    marker.remove();
                                    MarkerOptions markerOptions = new MarkerOptions();
                                    LatLng latLng = new LatLng(doc.getDouble("lat"), doc.getDouble("lng"));
                                    markerOptions.position(latLng);
                                    markerOptions.title(doc.getString("username"));
                                    marker = mMap.addMarker(markerOptions);
                                    MySingelton.getInstance().markerHashMap.put(doc.getString("username"), marker);
                                    GetAccessToken(getContext(), trackID, doc.getString("username"));


                                } else {
                                    MarkerOptions markerOptions = new MarkerOptions();
                                    LatLng latLng = new LatLng(doc.getDouble("lat"), doc.getDouble("lng"));
                                    markerOptions.position(latLng);
                                    markerOptions.title(doc.getString("username"));
                                    Marker marker = mMap.addMarker(markerOptions);
                                    MySingelton.getInstance().markerHashMap.put(doc.getString("username"), marker);

                                    GetAccessToken(getContext(), trackID, doc.getString("username"));

                                }

                                Log.d("STITCH", "Lat" + doc.getDouble("lat"));
                            }
                        }
                    } else {
                    }
                    return;
                }
                Log.e("STITCH", "Error: " + task.getException().toString());
                task.getException().printStackTrace();
            }
        });

    }

    void PushMyDataToMongo() {
        final StitchAppClient client =
                Stitch.getAppClient("findmyhomie-etrmr");

        final RemoteMongoClient mongoClient =
                client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");

        final RemoteMongoCollection<Document> _remoteCollection =
                mongoClient.getDatabase("Android").getCollection("users");

        client.getAuth().loginWithCredential(new AnonymousCredential()).continueWithTask(
                new Continuation<StitchUser, Task<RemoteUpdateResult>>() {

                    @Override
                    public Task<RemoteUpdateResult> then(@NonNull Task<StitchUser> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Log.e("STITCH", "Login failed!");
                            throw task.getException();
                        }
                        UserRepository userRepository = new UserRepository(getContext());

                        String myUsername = userRepository.getAllUsers().get(0).username;
                        final Document updateDoc = new Document();
                        if (MySingelton.getInstance().markerHashMap.containsKey(myUsername)) {
                            SpotifySongData songData = (SpotifySongData) MySingelton.getInstance().markerHashMap.get(myUsername).getTag();
                            LatLng latlng = MySingelton.getInstance().markerHashMap.get(myUsername).getPosition();

                            updateDoc.put("username", myUsername);
                            updateDoc.put("trackID", songData.trackID);
                            updateDoc.put("lat", latlng.latitude);
                            updateDoc.put("lng", latlng.longitude);
                            return _remoteCollection.updateOne(
                                    Filters.eq("username", myUsername.trim()), updateDoc, new RemoteUpdateOptions().upsert(true)
                            );
                        } else {
                            return _remoteCollection.updateOne(
                                    Filters.eq("username", "asdasdasd2323#$@#%#@"), updateDoc, new RemoteUpdateOptions().upsert(false)
                            );
                        }

                    }
                }
        ).continueWithTask(new Continuation<RemoteUpdateResult, Task<List<Document>>>() {
            @Override
            public Task<List<Document>> then(@NonNull Task<RemoteUpdateResult> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.e("STITCH", "Update failed!");
                    throw task.getException();
                }
                List<Document> docs = new ArrayList<>();
                return _remoteCollection
                        .find()
                        .into(docs);
            }
        }).addOnCompleteListener(new OnCompleteListener<List<Document>>() {
            @Override
            public void onComplete(@NonNull Task<List<Document>> task) {
                if (task.isSuccessful()) {
                    Log.d("STITCH", "Found docs: " + task.getResult().toString());
                    return;
                }
                Log.e("STITCH", "Error: " + task.getException().toString());
                task.getException().printStackTrace();
            }
        });

    }

    void RenderMarkers() {
        for (Map.Entry<String, Marker> element : MySingelton.getInstance().markerHashMap.entrySet()) {
            mMap.clear();
            LatLng latLng = element.getValue().getPosition();
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(element.getValue().getTitle());
            mMap.addMarker(markerOptions);

        }
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }
}



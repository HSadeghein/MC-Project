package com.example.findmyhomie;

import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;

public class MySingelton {
    private static MySingelton mInstance= null;

    public HashMap<String, Marker> markerHashMap;

    protected MySingelton(){
        markerHashMap = new HashMap<String, Marker>();
    }

    public static synchronized MySingelton getInstance() {
        if(null == mInstance){
            mInstance = new MySingelton();
        }
        return mInstance;
    }
}
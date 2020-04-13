package com.example.findmyhomie;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = {"username"},
        unique = true)})
public class User {
    @PrimaryKey(autoGenerate = true)
    public int uid;
    @ColumnInfo(name = "username")
    public String username;
    @ColumnInfo(name = "full_name")
    public String fullName;
    @ColumnInfo(name = "lat")
    public float Lat;
    @ColumnInfo(name = "lng")
    public float Lng;
    @ColumnInfo(name = "spotify_current_song_id")
    public String spotifySongID;

    public void setUsername(String _username) {
        this.username = _username;
    }

    public String getUsername() {
        return this.username;
    }

    public void setFullName(String _fullName) {
        this.fullName = _fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setLat(float _Lat) {
        this.Lat = _Lat;
    }

    public float getLat() {
        return this.Lat;
    }

    public void setLng(float _Lng) {
        this.Lng = _Lng;
    }

    public float getLng() {
        return this.Lng;
    }

    public void setSpotifySongID(String _id) {
        this.spotifySongID = _id;
    }

    public String getSpotifyUsername() {
        return this.spotifySongID;
    }

}

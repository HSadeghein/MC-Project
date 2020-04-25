package com.example.findmyhomie;

import java.io.Serializable;

public class SpotifySongData implements Serializable {
    public String trackID;
    public String Album;
    public String Artist;
    public String imgURL;
    public String uri;
    public String url;
    public String name;
    public String username;

    public SpotifySongData() {
        trackID = "";
        Album = "";
        Artist = "";
        imgURL = "";
        uri = "";
        url = "";
        name = "";
        username = "";
    }

}

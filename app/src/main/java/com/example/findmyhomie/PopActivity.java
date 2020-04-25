package com.example.findmyhomie;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class PopActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width, height;
        width = dm.widthPixels;
        height = dm.heightPixels;

        getWindow().setLayout((int) (width * .8), (int) (height * 0.6));

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = -20;
        params.alpha = 0.85f;
        params.dimAmount = 0.3f;
        getWindow().setAttributes(params);

        SpotifySongData songData = (SpotifySongData) getIntent().getSerializableExtra("SpotifySongData");
        ImageView imageView = (ImageView) findViewById(R.id.img_track);
        Picasso.get().load(songData.imgURL).into(imageView);
        TextView textView = (TextView) findViewById(R.id.txt_name);
        textView.setText(songData.username);
        textView = (TextView) findViewById(R.id.txt_track_name);
        textView.setText(songData.name);
        textView = (TextView) findViewById(R.id.txt_album_name);
        textView.setText(songData.Album);
        textView = (TextView) findViewById(R.id.txt_artist_name);
        textView.setText(songData.Artist);
        final Button button = findViewById(R.id.btn_spotify);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Uri uri = Uri.parse(songData.url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }
}

package com.example.findmyhomie;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

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

        getWindow().setLayout((int) (width * .6), (int) (height * 0.4));

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = -20;
        params.alpha = 0.85f;
        params.dimAmount = 0.3f;
        getWindow().setAttributes(params);
    }
}

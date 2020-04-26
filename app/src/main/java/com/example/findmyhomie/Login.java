package com.example.findmyhomie;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Random;

public class Login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        final Button button = findViewById(R.id.btn_login);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UserRepository userRepository = new UserRepository(getApplicationContext());
                User user = new User();
                final EditText txtUsername = findViewById(R.id.edittxt_username);
                if(!txtUsername.getText().toString().equals(""))
                    user.username = txtUsername.getText().toString();
                else {
                    byte[] array = new byte[7]; // length is bounded by 7
                    new Random().nextBytes(array);
                    String generatedString = new String(array, Charset.forName("UTF-8"));

                    user.username = generatedString;
                }
                userRepository.insertUser(user);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", (Serializable) user);
                setResult(Activity.RESULT_OK,returnIntent);
                finish();

            }
        });
    }


}

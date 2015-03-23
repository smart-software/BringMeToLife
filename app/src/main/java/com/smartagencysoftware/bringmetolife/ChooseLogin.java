package com.smartagencysoftware.bringmetolife;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;

import info.hoang8f.widget.FButton;

/**
 * Created by Alexey on 23.03.2015.
 */
public class ChooseLogin extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        FButton loginAn = (FButton) findViewById(R.id.loginAn);
        FButton loginOther = (FButton) findViewById(R.id.loginOther);

        loginAn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseAnonymousUtils.logIn(new LogInCallback() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        if(e!=null){
                            Intent intent = new Intent(getApplicationContext(), BringMeToLifeMainActivity.class);
                            startActivity(intent);
                        }
                        else {
                            Log.d("ChooseLogin","Failed login Anonymously.");
                        }
                    }
                });
            }
        });
        loginOther.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
    }

}

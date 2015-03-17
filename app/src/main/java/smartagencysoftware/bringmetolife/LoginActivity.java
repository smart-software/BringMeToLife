package smartagencysoftware.bringmetolife;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

/**
 * Created by Alexey on 12.01.2015.
 */
public class LoginActivity extends Activity {

    private EditText mUsername;
    private EditText mPassword;
    private info.hoang8f.widget.FButton mLoginButton;
    private TextView mSignUpTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(true);
        if(ParseUser.getCurrentUser() !=null){
            gotoMainActivity();
        }
        setContentView(R.layout.activity_login);


        mSignUpTextView = (TextView)findViewById(R.id.signUpText);
        mSignUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
        mUsername = (EditText)findViewById(R.id.usernameField);
        mPassword = (EditText)findViewById(R.id.passwordField);
        mLoginButton = (info.hoang8f.widget.FButton) findViewById(R.id.loginButton);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = mUsername.getText().toString();
                String password = mPassword.getText().toString();

                username = username.trim();
                password = password.trim();

                if (username.isEmpty() || password.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    builder.setMessage(getString(R.string.alert_login_or_pass_empty))
                            .setTitle(getString(R.string.alert_error_message))
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                else {
                    // Login
                    //setProgressBarIndeterminateVisibility(true);

                    ParseUser.logInInBackground(username, password, new LogInCallback() {
                        @Override
                        public void done(ParseUser user, ParseException e) {
                            //setProgressBarIndeterminateVisibility(false);

                            if (e == null) {
                                // Success!
                                gotoMainActivity();
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                builder.setMessage(e.getMessage())
                                        .setTitle(getString(R.string.alert_login_error))
                                        .setPositiveButton(android.R.string.ok, null);
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        }
                    });
                }
            }
        });
        final Activity thisActivity = this;
        info.hoang8f.widget.FButton loginFBButton = (info.hoang8f.widget.FButton) findViewById(R.id.loginFB);
        loginFBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ParseFacebookUtils.logIn(thisActivity, new LogInCallback() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {

                        if (parseUser == null) {
                            Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");
                        } else if (parseUser.isNew()) {
                            Log.d("MyApp", "User signed up and logged in through Facebook!"+ParseUser.getCurrentUser());
                            gotoMainActivity();
                        } else {
                            Log.d("MyApp", "User logged in through Facebook!"+ParseUser.getCurrentUser());
                            gotoMainActivity();
                        }
                    }
                });
            }
        });
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if(ParseUser.getCurrentUser()==null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            builder.setView(inflater.inflate(R.layout.dialog_teach, null))
                    .setPositiveButton(android.R.string.ok, null);
            builder.create();
            builder.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Logs 'install' and 'app activate' App Events. Facebook
        //AppEventsLogger.activateApp(this);
    }

    private void gotoMainActivity() {
        Intent intent = new Intent(this, MainActivityMipe.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // new fb sdk 3.0.2 has requestCode = 64206 in case that account is already linked to the parse and requestCode = 32665 if it is not linked and login is needed. It seems parse just a little bit obsolete in part of working in new fb sdk. It crashes on requestCode = 64206. Thanks Yuriy Umanets!
        if(requestCode == 64206){ //TODO get rid of this walkaround
            requestCode = 32665;
        }
        ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
    }

    private void checkUserAndgotoMain(){
        if (ParseUser.getCurrentUser()!=null){
            Log.d("checkUserAndgotoMain", "Username:" + ParseUser.getCurrentUser().getUsername());
            gotoMainActivity();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Logs 'app deactivate' App Event. Facebook
        //AppEventsLogger.deactivateApp(this);
    }
}

package com.smartagencysoftware.bringmetolife;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.Parse;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseCrashReporting;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;
import com.smartagencysoftware.bringmetolife.smartagencysoftware.bringmetolife.service.BringMeToLifeService;

import java.util.List;


public class BringMeToLifeMainActivity extends ActionBarActivity {

    public static Activity mainActivity;
    static Context context;
    static  Handler uiHandler = new Handler();
    private TextView fullUsername;
    private TextView socialRank;
    private MapFragment mGoogleMapFragment;
    private static GoogleMap mGoogleMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bring_me_to_life_main);
        mainActivity = this;
        context = getApplicationContext();
        Parse.enableLocalDatastore(this);
        ParseCrashReporting.enable(this);
        Parse.initialize(this, "F13jhzTNsPglWJ3rSXIFjPlKhcvPVuUmzqhkdsxd", "vHGFSAN2uaoKpPPFsn19Jm3WjaBW7iBFD7asCnqv");
        ParseUser.enableAutomaticUser();
        if (!isMyServiceRunning(BringMeToLifeService.class)){ // This check is on the off-chance
            startService( new Intent(this, BringMeToLifeService.class));
        }
        if(ParseUser.getCurrentUser() == null){ //barely possible with AutomaaticUser enabled
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        fullUsername = (TextView)findViewById(R.id.fullusername);
        socialRank = (TextView)findViewById(R.id.socialrank);
        mGoogleMapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mGoogleMap = mGoogleMapFragment.getMap();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (ParseAnonymousUtils.isLinked(currentUser)){
            fullUsername.setText("Anonymous");
            if(currentUser.getObjectId()==null){
                currentUser.saveInBackground(); //TODO savecallback with progress bar and message.
            }
        }
        else {
            fullUsername.setText(currentUser.getUsername());
            socialRank.setText(currentUser.getString("socialRank"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bring_me_to_life_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                break;
            case R.id.action_checkFriends:
                sendToService("checkFriends");
                break;
        }

        return super.onOptionsItemSelected(item);
    }



    public static void postInHandler(final String string){
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(context,
                        string, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    public static void postFriends(final List<ParseUser> friends){
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (friends.size()==0){
                    return;
                }
                ParseGeoPoint tempGeopoint =null;
                mGoogleMap.setMyLocationEnabled(true);
                for(ParseUser friend:friends){
                    tempGeopoint = friend.getParseGeoPoint("location");
                    mGoogleMap.addMarker(new MarkerOptions()
                    .title(friend.getUsername())
                    .position(new LatLng(tempGeopoint.getLatitude(),tempGeopoint.getLongitude())));
                }
                //move camera to last friend in list. Must center the user position instead TODO
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(tempGeopoint.getLatitude(),tempGeopoint.getLongitude())));
            }
        });

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void sendToService(String command){
        Intent intent = new Intent();
        intent.setAction("com.smartagencysoftware.bringmetolife.service.receiver.checkfriends.main");
        sendBroadcast(intent);
    }


}

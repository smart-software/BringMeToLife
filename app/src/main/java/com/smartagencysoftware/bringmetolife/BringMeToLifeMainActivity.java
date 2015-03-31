package com.smartagencysoftware.bringmetolife;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.GetCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.smartagencysoftware.bringmetolife.smartagencysoftware.bringmetolife.service.BringMeToLifeService;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class BringMeToLifeMainActivity extends ActionBarActivity {

    private static final int REQUEST_IMAGE_CHOOSE = 2;
    public static Activity mainActivity;
    static Context context;
    static  Handler uiHandler = new Handler();
    private TextView fullUsername;
    private TextView socialRank;
    private MapFragment mGoogleMapFragment;
    private static GoogleMap mGoogleMap;
    private LocationManager mLocationManager;
    private Location lastKnownLocation;
    private CircleImageView mAvatar;
    private Menu mMenu;
    private XYPlot statsPlot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bring_me_to_life_main);
        mainActivity = this;
        context = getApplicationContext();
        if (!isMyServiceRunning(BringMeToLifeService.class)){ // This check is on the off-chance
            startService( new Intent(this, BringMeToLifeService.class));
        }
        if(ParseUser.getCurrentUser() == null){ //barely possible with AutomaticUser enabled
            Intent intent = new Intent(this, Choose.class);
            startActivity(intent);
        }

        fullUsername = (TextView)findViewById(R.id.fullusername);
        socialRank = (TextView)findViewById(R.id.socialrank);
        mAvatar = (CircleImageView)findViewById(R.id.avatar);
        mGoogleMapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mGoogleMap = mGoogleMapFragment.getMap();

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);


        mAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, REQUEST_IMAGE_CHOOSE);
            }
        });

        setUpStatsGraph();
        Number[] numSightings = {5, 8, 9, 2, 5};
        Number[] years = {
                978307200,  // 2001
                1009843200, // 2002
                1041379200, // 2003
                1072915200, // 2004
                1104537600  // 2005
        };
        setPlotData(years,numSightings);
    }



    @Override
    protected void onStart() {
        super.onStart();

    }


    @Override
    protected void onResume() {
        super.onResume();

        lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(lastKnownLocation!=null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())));
            mGoogleMap.moveCamera(CameraUpdateFactory.zoomBy(20));
        }
        final ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser!=null){
            if (ParseAnonymousUtils.isLinked(currentUser)){
                fullUsername.setText("Anonymous");
                if(currentUser.getObjectId()==null){
                    currentUser.saveInBackground(); //TODO savecallback with progress bar and message.
                }
            }
            else {
                fullUsername.setText(currentUser.getUsername());
                socialRank.setText(currentUser.getString("socialRank"));
                ParseUser.getCurrentUser().fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                    @Override
                    public void done(ParseObject parseObject, ParseException e) {
                        try {
                            String avatarUrl = currentUser.getParseFile("avatar").getUrl();
                            Picasso.with(getApplicationContext()).load(avatarUrl).into(mAvatar);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                });

            }
        }
        menuInflate(mMenu); //must be in onNavigateUpFromChild, but this method dont fires. TODO
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMenu = menu;
        menuInflate(menu);
        return true;
    }


    private void menuInflate(Menu menu) {
        if(mMenu==null){return;};
        mMenu.clear();
        MenuInflater menuInflater = getMenuInflater();

        ParseUser currentUser = ParseUser.getCurrentUser();
        if(currentUser!=null && ParseAnonymousUtils.isLinked(currentUser)){
            menuInflater.inflate(R.menu.menu_bring_me_to_life_main_anonymous, menu);
        }
        else if(currentUser!=null){
            menuInflater.inflate(R.menu.menu_bring_me_to_life_main, menu);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent; //TODO redo
        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                break;
            case R.id.action_checkFriends:
                sendToService("checkFriends");
                break;
            case R.id.action_signup:
                intent = new Intent(this, Choose.class);
                startActivity(intent);
                break;
            case R.id.action_logoff:
                ParseUser.logOut();
                intent = new Intent(this, Choose.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CHOOSE && resultCode == RESULT_OK) {
            Bitmap bitmap = null;
            try {
                InputStream stream = getContentResolver().openInputStream(
                        data.getData());
                bitmap = BitmapFactory.decodeStream(stream);
                stream.close();
                mAvatar.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(bitmap!=null){
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                final ParseFile avatarPFile = new ParseFile("avatar",byteArray);
                avatarPFile.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        ParseUser.getCurrentUser().put("avatar", avatarPFile);
                    }
                });
            }
        }
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
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomBy(20));
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


    private void setPlotData(Number[] years, Number[] numSightings){

        // create our series from our array of nums:
        XYSeries series2 = new SimpleXYSeries(
                Arrays.asList(years),
                Arrays.asList(numSightings),
                "Social stats");

        // Create a formatter to use for drawing a series using LineAndPointRenderer:
        LineAndPointFormatter series2Format = new LineAndPointFormatter(Color.rgb(0,100,0),Color.rgb(0, 100, 0),                   // point color
                Color.rgb(100, 200, 0), new PointLabelFormatter());
        LineAndPointFormatter formatter  = new LineAndPointFormatter(Color.rgb(0, 0,0), Color.BLUE, Color.RED, new PointLabelFormatter());

        // draw a domain tick for each year:
        statsPlot.setDomainStep(XYStepMode.SUBDIVIDE, years.length);
        // setup our line fill paint to be a slightly transparent gradient:
        Paint lineFill = new Paint();
        lineFill.setAlpha(200);
        lineFill.setShader(new LinearGradient(0, 0, 0, 250, Color.WHITE, Color.GREEN, Shader.TileMode.MIRROR));

        formatter.setFillPaint(lineFill);

        statsPlot.addSeries(series2, formatter);
    }

    private void setUpStatsGraph(){
        statsPlot = (XYPlot) findViewById(R.id.statsPlot);

        statsPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
        statsPlot.getGraphWidget().getDomainGridLinePaint().setColor(Color.BLACK);
        statsPlot.getGraphWidget().getDomainGridLinePaint().setPathEffect(new DashPathEffect(new float[]{1, 1}, 1));
        statsPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        statsPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);

        statsPlot.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
        statsPlot.getBorderPaint().setStrokeWidth(1);
        statsPlot.getBorderPaint().setAntiAlias(false);
        statsPlot.getBorderPaint().setColor(Color.WHITE);



        statsPlot.getGraphWidget().setPaddingRight(2);

        // customize our domain/range labels
        statsPlot.setDomainLabel("Date");
        statsPlot.setRangeLabel("Time spent");

        // get rid of decimal points in our range labels:
        statsPlot.setRangeValueFormat(new DecimalFormat("0"));

        statsPlot.setDomainValueFormat(new Format() {

            // create a simple date format that draws on the year portion of our timestamp.
            // see http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html
            // for a full description of SimpleDateFormat.
            private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, ''yy");

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {

                // because our timestamps are in seconds and SimpleDateFormat expects milliseconds
                // we multiply our timestamp by 1000:
                long timestamp = ((Number) obj).longValue() * 1000;
                Date date = new Date(timestamp);
                return dateFormat.format(date, toAppendTo, pos);
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;

            }
        });

        /*// by default, AndroidPlot displays developer guides to aid in laying out your plot.
        // To get rid of them call disableAllMarkup():
        statsPlot.disableAllMarkup();*/
    }

}

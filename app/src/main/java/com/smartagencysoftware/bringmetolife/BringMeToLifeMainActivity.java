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
import android.graphics.PointF;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
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
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.smartagencysoftware.bringmetolife.com.smartagencysoftware.bringmetolife.misc.constants;
import com.smartagencysoftware.bringmetolife.smartagencysoftware.bringmetolife.service.BringMeToLifeService;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
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
    private PointF minXY;
    private PointF maxXY;
    private JSONObject mStats;
    private Button mRefreshStats;
    private Button mRefreshFriends;


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
        mRefreshStats = (Button) findViewById(R.id.refreshStats);
        mRefreshFriends = (Button) findViewById(R.id.refreshFriends);
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

        mRefreshStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getStats(new StatsCallback());
            }
        });
        mRefreshFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToService("checkFriends");
            }
        });
        setUpStatsGraph();
    }



    @Override
    protected void onStart() {
        super.onStart();

    }


    @Override
    protected void onResume() {
        super.onResume();
        final ParseUser currentUser = ParseUser.getCurrentUser();
        lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(lastKnownLocation!=null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())));
            mGoogleMap.moveCamera(CameraUpdateFactory.zoomBy(20));
        }



        if (currentUser!=null){
            if (ParseAnonymousUtils.isLinked(currentUser)){
                fullUsername.setText("Anonymous");
                if(currentUser.getObjectId()==null){
                    currentUser.saveInBackground(); //TODO savecallback with progress bar and message.
                }
            }
            else {
                fullUsername.setText(currentUser.getUsername());
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
        getStats(new StatsCallback());
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
            case R.id.action_addfriends:
                intent = new Intent(this, FriendsActivity.class);
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


    private void setPlotData(ArrayList<Long> date, ArrayList<Integer> timeSpent){
        statsPlot.clear();
        // create our series from our array of nums:
        XYSeries series2 = new SimpleXYSeries(
                (List)date,
                (List)timeSpent,
                "Social stats");
        // Create a formatter to use for drawing a series using LineAndPointRenderer:
        LineAndPointFormatter series2Format = new LineAndPointFormatter(Color.rgb(0,100,0),Color.rgb(0, 100, 0),                   // point color
                Color.rgb(100, 200, 0), new PointLabelFormatter(Color.BLACK));
        LineAndPointFormatter formatter  = new LineAndPointFormatter(Color.rgb(0, 0,0), Color.BLUE, Color.RED, new PointLabelFormatter(Color.BLACK));
        // draw a domain tick for each year:
        statsPlot.setDomainStep(XYStepMode.SUBDIVIDE, date.size());
        // setup our line fill paint to be a slightly transparent gradient:
        Paint lineFill = new Paint();
        lineFill.setAlpha(200);
        lineFill.setShader(new LinearGradient(0, 0, 0, 250, Color.WHITE, Color.GREEN, Shader.TileMode.MIRROR));

        formatter.setFillPaint(lineFill);

        statsPlot.addSeries(series2, formatter);
        //Set of internal variables for keeping track of the boundaries
        statsPlot.calculateMinMaxVals();
        minXY=new PointF(statsPlot.getCalculatedMinX().floatValue(),statsPlot.getCalculatedMinY().floatValue());
        maxXY=new PointF(statsPlot.getCalculatedMaxX().floatValue(),statsPlot.getCalculatedMaxY().floatValue());
        statsPlot.redraw();
    }

    private void setUpStatsGraph(){
        statsPlot = (XYPlot) findViewById(R.id.statsPlot);
        statsPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.TRANSPARENT);
        statsPlot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
        statsPlot.getGraphWidget().getDomainGridLinePaint().setColor(Color.BLACK);
        statsPlot.getGraphWidget().getDomainGridLinePaint().setPathEffect(new DashPathEffect(new float[]{1, 1}, 1));
        statsPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        statsPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);

        statsPlot.getGraphWidget().setGridPadding(50, 50, 50, 50);
        statsPlot.setPlotMargins(0, 0, 0, 0);
        statsPlot.setPlotPadding(0, 0, 0, 0);
        statsPlot.setGridPadding(0, 10, 5, 0);
        statsPlot.setBorderStyle(Plot.BorderStyle.NONE, null, null);
        statsPlot.getGraphWidget().getRangeLabelPaint().setTextSize(PixelUtils.dpToPix(12));
        statsPlot.getGraphWidget().getDomainLabelPaint().setTextSize(PixelUtils.dpToPix(9));

        statsPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.BLACK);
        statsPlot.getGraphWidget().getRangeLabelPaint().setColor(Color.BLACK);

        statsPlot.getDomainLabelWidget().getLabelPaint().setColor(Color.BLACK);
        statsPlot.getRangeLabelWidget().getLabelPaint().setColor(Color.BLACK);
        statsPlot.getRangeLabelWidget().getLabelPaint().setTextSize(PixelUtils.dpToPix(12));

        statsPlot.getDomainLabelWidget().getLabelPaint().setTextAlign(Paint.Align.LEFT);
        statsPlot.getBorderPaint().setStrokeWidth(1);
        statsPlot.getBorderPaint().setAntiAlias(false);
        statsPlot.getBorderPaint().setColor(Color.BLACK);

        statsPlot.setBorderStyle(Plot.BorderStyle.NONE, null, null);
        statsPlot.setGridPadding(0, 10, 5, 0);


        Paint paTitleColor = new Paint();
        paTitleColor.setAlpha(0);
        paTitleColor.setColor(Color.BLACK);
        statsPlot.getDomainLabelWidget().setLabelPaint(paTitleColor);

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
            private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d");

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {

                long timestamp = ((Number) obj).longValue();
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

    private void getStats(final StatsCallback callback){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("stats");
        query.whereEqualTo("parent", ParseUser.getCurrentUser().getObjectId());
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                  if(e==null) {
                      callback.onExecute(parseObject.getJSONObject("stats"));
                  }
            }
        });
    }

    private class StatsCallback{
        void onExecute(JSONObject stats){
            mStats = stats;
            socialRank.setText(calcSocialStatus(mStats));
            setPlotData(stats, "overall");
        }
    }

    private void setPlotData(JSONObject stats, String plotType) {
        if(stats ==null) {return;}
        Iterator iterator = stats.keys();
        ArrayList<Long> date = new ArrayList<Long>();
        ArrayList<Integer> timeSpent = new ArrayList<Integer>();
        String tmpKey = null;
        JSONObject tmpJson = null;
        while(iterator.hasNext()){
            tmpKey = (String) iterator.next();
            date.add(Long.valueOf(tmpKey));

        }

        Collections.sort(date);

        for (Long tempDate: date){
            try {
                tmpJson = stats.getJSONObject(String.valueOf(tempDate)); //TODO tooo much String>Long>String
                timeSpent.add((Integer) tmpJson.get(plotType));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        setPlotData(date, timeSpent);
    }

    private String calcSocialStatus(JSONObject stats){
        if (stats == null){
            return "beginner";
        }
        Iterator iterator = stats.keys();
        ArrayList<Long> date = new ArrayList<Long>();
        ArrayList<Integer> timeSpent = new ArrayList<Integer>();
        String tmpKey = null;
        JSONObject tmpJson = null;
        int overalltimeSocial=0;
        float  socialRankfactor = 0;
        int overallTime = 0;
        String socialRankString = null;

        while(iterator.hasNext()){
            tmpKey = (String) iterator.next();
            date.add(Long.valueOf(tmpKey));

        }

        Collections.sort(date);

        for (Long tempDate: date){
            try {
                tmpJson = stats.getJSONObject(String.valueOf(tempDate)); //TODO tooo much String>Long>String
                timeSpent.add((Integer) tmpJson.get(constants.fb)); //TODO redo
                timeSpent.add((Integer) tmpJson.get(constants.vk));
                timeSpent.add((Integer) tmpJson.get(constants.ok));
                timeSpent.add((Integer) tmpJson.get(constants.in));
                timeSpent.add((Integer) tmpJson.get(constants.vi));
                timeSpent.add((Integer) tmpJson.get(constants.wa));
                overallTime += (int) tmpJson.get(constants.ov);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for (Integer time:timeSpent){
            overalltimeSocial+=time;
        }

        socialRankfactor = overalltimeSocial/overallTime;
        return calcRank(socialRankfactor);
    }

    private String calcRank(float factor){
        if (factor>=0 & factor<0.1){return "begginer";}
        if (factor>=0.1 & factor<0.2){return "concerned";}
        if (factor>=0.2 & factor<0.3){return "occasional guest";}
        if (factor>=0.3 & factor<0.4){return "involved";}
        if (factor>=0.4 & factor<0.5){return "enthusiastic";}
        if (factor>=0.5 & factor<0.6){return "begginer";}
        if (factor>=0.6 & factor<0.7){return "venturesome";}
        if (factor>=0.8 & factor<0.8){return "social prisoner";}
        if (factor>=0.9 & factor<=1){return "social animal";}
        return "none";
    }

}

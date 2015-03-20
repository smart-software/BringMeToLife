package com.smartagencysoftware.bringmetolife.smartagencysoftware.bringmetolife.service;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.FunctionCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.smartagencysoftware.bringmetolife.BringMeToLifeMainActivity;

import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;

public class BringMeToLifeService extends Service {
    private static Handler uiHandler = new Handler();
    private Location lastKnownLocation = null;
    private Location oldLastKnownLocation = null;
    private ActivityManager mActivityManager = null;
    private GoogleApiClient mGoogleApiClient = null;
    private ScreenReceiver mScreenReceiver;

    //social time counters
    private int counterFB,counterIN,counterVK,counterWA,counterVI,counterOK,counterNone, counterOverall = 0;
    private boolean screenIsOn;
    private Timer timerSocial;

    public BringMeToLifeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Check if Google play Services is avaliable
        int resultCode =   GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

        switch (resultCode){
            case ConnectionResult.SUCCESS:
                break;
            case ConnectionResult.SERVICE_MISSING:
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
            case ConnectionResult.SERVICE_DISABLED:
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, BringMeToLifeMainActivity.mainActivity, 1);
                dialog.show();
                break;
            default:
                break;
        }

        mGoogleApiClient = createGoogleApiClient();
        mGoogleApiClient.connect();

        Log.d("mGogleApiclient Build", "mGoogleApiClient is "+mGoogleApiClient.isConnected());

    }

    synchronized GoogleApiClient createGoogleApiClient(){
        return new GoogleApiClient.Builder(this).addConnectionCallbacks(new ConnectionCallbacks()).addOnConnectionFailedListener(new ConnectionFailedListener()).addApi(LocationServices.API).build();

    }

    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected(Bundle bundle) {
            oldLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            Log.d("onConnected", "fired");
            startLocationUpdates();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d("onConnected", "suspend");
        }
    }

    private void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, createLocationRequest(), createLocationListener());
    }

    private LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000*60*7); // release:1000*60*7 debug: 10*1000
        //mLocationRequest.setFastestInterval(1000*60*3);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        return mLocationRequest;
    }

    private LocationListener createLocationListener(){
     return new LocationChangeListener();
    }


    private class LocationChangeListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Log.d("LocationChangeListener","fired!");
            checkLife();
        }
    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener{

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d("onConnectionFailed","fired!" + connectionResult);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("StartService", "intent is "+intent);
        /*Timer myTimer = new Timer();
        myTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                CheckLifeTask checkLife = new CheckLifeTask();
                checkLife.execute();
            }}, 1000L, 60L * 1000*10); // before first launch: 1 sec, launch every 10 minutes*/
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenStateFilter.addAction("com.smartagencysoftware.bringmetolife.service.receiver.checkfriends");
        mScreenReceiver = new ScreenReceiver();
        registerReceiver(mScreenReceiver, screenStateFilter);
        screenIsOn = true;
        createSocialTimer(); // assume that at service start the screen is ON

        return super.onStartCommand(intent, flags, startId);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenReceiver);
    }

    class CheckLifeTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... noargs) {
            return checkLife();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            //txtResult.setText(result);
        }
    }

    class CheckSocialAppsAsync extends AsyncTask<Void, Void, Void> {
        private int secsAdd;
        CheckSocialAppsAsync(int secsAdd) {
            this.secsAdd = secsAdd;
        }

        @Override
        protected Void doInBackground(Void... noargs) {
            String socialNetwork = checkSocialApps();
            switch (socialNetwork){
                case "facebook":
                    counterFB+=secsAdd;
                    break;
                case "instagram":
                    counterIN+=secsAdd;
                case "vkontakte":
                    counterVK+=secsAdd;
                case "whatsapp":
                    counterWA+=secsAdd;
                    break;
                case "viber":
                    counterVI+=secsAdd;
                    break;
                case "odnoklassniki":
                    counterOK+=secsAdd;
                    break;
                case "none":
                    counterNone+=secsAdd;
                    break;
            }
            counterOverall+=secsAdd;
            Log.d("CheckSocialAppsAsync", "Social network: "+socialNetwork+"+with secs added: "+secsAdd);
            return null;
        }
    }

    private boolean checkLife(){
        // wonder if mGoogleApiClient is ready by now...  TODO
        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        BringMeToLifeMainActivity.postInHandler("last know location is "+lastKnownLocation);
        if (oldLastKnownLocation!=null){
            float distance = lastKnownLocation.distanceTo(oldLastKnownLocation);
            //DEBUG
            if (distance > 5) launchParseCheckNearFriends();
        }
        oldLastKnownLocation = lastKnownLocation;
        return true; //true == checkLife started
    }

    private void launchParseCheckNearFriends() {
        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient); //for compatibility with direct call. TODO redo
        //five meters
        double latitude = lastKnownLocation.getLatitude();
        double longitude = lastKnownLocation.getLongitude();
        ParseGeoPoint point = new ParseGeoPoint(latitude, longitude);
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("geopoint", point);
        params.put("facebook", counterFB);
        params.put("instagram", counterIN);
        params.put("vkontakte", counterVK);
        params.put("whatsapp", counterWA);
        params.put("viber", counterVI);
        params.put("odnoklassniki", counterOK);
        params.put("none", counterNone);
        params.put("overall",counterOverall);

        ParseCloud.callFunctionInBackground("checkNearFriends", params, new FunctionCallback<List<ParseUser>>() {
            public void done(List<ParseUser> result, ParseException e) {
                if (e == null) {
                    for (ParseUser friend : result) {
                        Log.d("checkNearFriends", "success: " + friend);

                    }
                    BringMeToLifeMainActivity.postInHandler("You have " + result.size() + " friends near: " + result);
                    counterFB = 0;
                    counterIN = 0;
                    counterVK = 0;
                    counterWA = 0;
                    counterVI = 0;
                    counterOK = 0;
                    counterNone = 0;
                    counterOverall = 0;

                }
            }
        });
    }

    private String checkSocialApps() {
        String appProcesName = getForegroundApp().processName;
        String result = "none";
        switch (appProcesName){
            case "com.facebook.katana":
                result = "facebook";
                break;
            case "com.instagram.android":
                result = "instagram";
                break;
            case "com.vkontakte.android":
                result = "vkontakte";
                break;
            case "com.whatsapp":
                result = "whatsapp";
                break;
            case "com.viber.voip":
                result = "viber";
                break;
            case "ru.ok.android":
                result = "odnoklassniki";
                break;
        }
        return result;
    }

    private RunningAppProcessInfo getForegroundApp() {
        RunningAppProcessInfo result= null;
        RunningAppProcessInfo info=null;

        if(mActivityManager==null)
            mActivityManager = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List <RunningAppProcessInfo> l = mActivityManager.getRunningAppProcesses();
        Iterator<RunningAppProcessInfo> i = l.iterator();

        while(i.hasNext()){
            info = i.next();
            if(info.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    /*&& !isRunningService(info.processName)*/){
                result=info;
                return result;
            }
        }
        return result;
    }

    private ComponentName getActivityForApp(RunningAppProcessInfo target){
        ComponentName result=null;
        ActivityManager.RunningTaskInfo info;

        if(target==null)
            return null;

        if(mActivityManager==null)
            mActivityManager = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List <ActivityManager.RunningTaskInfo> l = mActivityManager.getRunningTasks(9999);
        Iterator <ActivityManager.RunningTaskInfo> i = l.iterator();

        while(i.hasNext()){
            info=i.next();
            if(info.baseActivity.getPackageName().equals(target.processName)){
                result=info.topActivity;
                break;
            }
        }

        return result;
    }

    private boolean isStillActive(RunningAppProcessInfo process, ComponentName activity)
    {
        // activity can be null in cases, where one app starts another. for example, astro
        // starting rock player when a move file was clicked. we dont have an activity then,
        // but the package exits as soon as back is hit. so we can ignore the activity
        // in this case
        if(process==null)
            return false;

        RunningAppProcessInfo currentFg=getForegroundApp(); // getForegroundApp().get(0) - ONLY FOR REVERSE COMPATIBILITY
        ComponentName currentActivity=getActivityForApp(currentFg);

        if(currentFg!=null && currentFg.processName.equals(process.processName) &&
                (activity==null || currentActivity.compareTo(activity)==0))
            return true;

        /*Slog.i(TAG, "isStillActive returns false - CallerProcess: " + process.processName + " CurrentProcess: "
                + (currentFg==null ? "null" : currentFg.processName) + " CallerActivity:" + (activity==null ? "null" : activity.toString())
                + " CurrentActivity: " + (currentActivity==null ? "null" : currentActivity.toString()));
                */
        return false;
    }

    private boolean isRunningService(String processname){
        if(processname==null || processname.isEmpty())
            return false;

        ActivityManager.RunningServiceInfo service;

        if(mActivityManager==null)
            mActivityManager = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List <RunningServiceInfo> l = mActivityManager.getRunningServices(9999);
        Iterator <RunningServiceInfo> i = l.iterator();
        while(i.hasNext()){
            service = i.next();
            if(service.process.equals(processname))
                return true;
        }

        return false;
    }

    public class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ScreenReceiver", "Received!");
            String code = intent.getAction();
            switch (code){
                case ACTION_SCREEN_ON:
                    screenIsOn = true;
                    createSocialTimer();
                    break;
                case ACTION_SCREEN_OFF:
                    screenIsOn = false;
                    timerSocial.cancel();
                    break;
                case "com.smartagencysoftware.bringmetolife.service.receiver.checkfriends":
                    Log.d("ScreenReceive", "received call !!! mGoogleApi is connecred: "+mGoogleApiClient.isConnected());

                    checkLife();
                    break;
                default:
                    break;
            }
        }
    }


    private void createSocialTimer() {
        timerSocial = new Timer();
        timerSocial.scheduleAtFixedRate(new TimerTask() {
            int[] intervals = {5,5,10,10,10,20,30,30,60};
            int currentInterval = 0;
            int intervalSecsLeft = 5; //for the first timer "run"

            @Override
            public void run() {

                if(screenIsOn==true){

                    intervalSecsLeft-=5;
                    if (intervalSecsLeft == 0){
                        CheckSocialAppsAsync checkSocialAppsAsync = new CheckSocialAppsAsync(intervals[currentInterval]);
                        checkSocialAppsAsync.execute();
                        currentInterval+= 1;
                        if(currentInterval>=intervals.length){
                            currentInterval = intervals.length-1; //magic! TODO redo. 5 - is interval for socialTimer
                        }
                        intervalSecsLeft = intervals[currentInterval];
                    }


                }
                else {
                    currentInterval = 0;
                    intervalSecsLeft = 0;
                }
            }
        }, 0L, 5L*1000); // before first launch: 0 sec, launch every 5 sec
    }

}

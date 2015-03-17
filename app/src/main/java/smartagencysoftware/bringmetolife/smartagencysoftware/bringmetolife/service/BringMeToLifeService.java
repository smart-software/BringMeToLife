package smartagencysoftware.bringmetolife.smartagencysoftware.bringmetolife.service;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.FunctionCallback;
import com.parse.Parse;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import smartagencysoftware.bringmetolife.BringMeToLifeMainActivity;

public class BringMeToLifeService extends Service {
    private Location lastKnownLocation = null;
    private Location oldLastKnownLocation = null;
    private ActivityManager mActivityManager = null;
    private GoogleApiClient mGoogleApiClient = null;

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

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "F13jhzTNsPglWJ3rSXIFjPlKhcvPVuUmzqhkdsxd", "vHGFSAN2uaoKpPPFsn19Jm3WjaBW7iBFD7asCnqv");

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

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(new ConnectionCallbacks())
                            .addOnConnectionFailedListener(new ConnectionFailedListener()).addApi(LocationServices.API)
                            .build();
    }

    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected(Bundle bundle) {
            startLocationUpdates();
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    }

    private void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, createLocationRequest(), createLocationListener());
    }

    private LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000*60*7);
        mLocationRequest.setFastestInterval(1000*60*3);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        return mLocationRequest;
    }

    private LocationListener createLocationListener(){
     return new LocationChangeListener();
    }


    private class LocationChangeListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            //something can happen here, if necessary TODO
        }
    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener{

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timer myTimer = new Timer();
        myTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                CheckLifeTask checkLife = new CheckLifeTask();
                checkLife.execute();
            }}, 1000L, 60L * 1000*10); // before first launch: 1 sec, launch every 10 minutes

        return super.onStartCommand(intent, flags, startId);
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

    private boolean checkLife(){
        // wonder if mGoogleApiClient is ready by now...  TODO
        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (oldLastKnownLocation!=null){
            float distance = lastKnownLocation.distanceTo(oldLastKnownLocation);
            if (distance > 5) { //five meters
                double latitude = lastKnownLocation.getLatitude();
                double longitude = lastKnownLocation.getLongitude();
                ParseGeoPoint point = new ParseGeoPoint(latitude, longitude);
                HashMap<String, ParseGeoPoint> params = new HashMap<String, ParseGeoPoint>();
                params.put("geopoint", point);
                ParseCloud.callFunctionInBackground("checkNearFriends", params, new FunctionCallback<List<ParseUser>>() {
                    public void done(List<ParseUser> result, ParseException e) {
                        if (e == null) {
                            for (ParseUser friend: result){
                                Log.d("checkNearFriends", "success: "+ friend);
                                ArrayList<RunningAppProcessInfo> appProcessInfo = getForegroundApp();
                                if(appProcessInfo!=null) {
                                    ArrayList<String> processInfo = new ArrayList<String>();
                                    for (RunningAppProcessInfo info: appProcessInfo){
                                        processInfo.add(info.processName);
                                    }
                                    if (processInfo.contains("com.facebook.katana")){
                                        BringMeToLifeMainActivity.postInHandler("Facebook is foreground!");
                                    }
                                    if (processInfo.contains("com.instagram.android")){
                                        BringMeToLifeMainActivity.postInHandler("Instagram is foreground!");
                                    }
                                    if (processInfo.contains("com.vkontakte.android")){
                                        BringMeToLifeMainActivity.postInHandler("Vkontakte is foreground!");
                                    }
                                    if (processInfo.contains("com.whatsapp")){
                                        BringMeToLifeMainActivity.postInHandler("WhatsApp is foreground!");
                                    }
                                    if (processInfo.contains("com.viber.voip")){
                                        BringMeToLifeMainActivity.postInHandler("Viber is foreground!");
                                    }
                                    if (processInfo.contains("ru.ok.android")){
                                        BringMeToLifeMainActivity.postInHandler("Odnoklassniki is foreground!");
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
        oldLastKnownLocation = lastKnownLocation;
        return true; //true == checkLife started
    }

    private ArrayList<RunningAppProcessInfo> getForegroundApp() {
        ArrayList<RunningAppProcessInfo> result= new ArrayList<RunningAppProcessInfo>();
        RunningAppProcessInfo info=null;

        if(mActivityManager==null)
            mActivityManager = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List <RunningAppProcessInfo> l = mActivityManager.getRunningAppProcesses();
        Iterator<RunningAppProcessInfo> i = l.iterator();

        while(i.hasNext()){
            info = i.next();
            if(info.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    /*&& !isRunningService(info.processName)*/){
                result.add(info);
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

        RunningAppProcessInfo currentFg=getForegroundApp().get(0); // getForegroundApp().get(0) - ONLY FOR REVERSE COMPATIBILITY
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

}

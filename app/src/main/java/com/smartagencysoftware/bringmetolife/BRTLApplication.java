package com.smartagencysoftware.bringmetolife;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseFacebookUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Alexey on 24.03.2015.
 */
public class BRTLApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.enableLocalDatastore(this);
        ParseCrashReporting.enable(this);
        Parse.initialize(this, "F13jhzTNsPglWJ3rSXIFjPlKhcvPVuUmzqhkdsxd", "vHGFSAN2uaoKpPPFsn19Jm3WjaBW7iBFD7asCnqv");
        ParseFacebookUtils.initialize(String.valueOf(R.string.app_id));

        //debug only for hashset of developer enviroment
        obtainDevKeyEnvoroment();
    }

    private void obtainDevKeyEnvoroment() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.smartagencysoftware.bringmetolife",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}

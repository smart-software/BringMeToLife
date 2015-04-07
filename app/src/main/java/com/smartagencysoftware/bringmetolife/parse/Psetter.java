package com.smartagencysoftware.bringmetolife.parse;

import android.os.AsyncTask;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.Objects;

/**
 * Created by Alexey on 07.04.2015.
 */
public class Psetter {

    public static void plusFriend(final String userId, final PsetterCallBack callBack){
        class Async extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... params) {
                ParseUser currentUser = ParseUser.getCurrentUser();
                try {
                    currentUser.fetchIfNeeded();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                currentUser.add("friendsId", userId);

                currentUser.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        callBack.run(e);
                    }
                });
                return null;
            }
        }
    }

    //misc
    public interface PsetterCallBack {
        void run(ParseException e);
    }
}

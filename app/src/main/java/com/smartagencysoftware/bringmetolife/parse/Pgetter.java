package com.smartagencysoftware.bringmetolife.parse;

import android.os.AsyncTask;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexey on 07.04.2015.
 */
public class Pgetter {
    public static void friends(final PgetterCallback callback){
        class Async extends AsyncTask<Object,Object,Object>{
            @Override
            protected Object doInBackground(Object... params) {
                ParseUser currentUser = ParseUser.getCurrentUser();
                List<ParseUser> friends = null;
                if (currentUser!=null){
                    try {
                        currentUser.fetch();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    List<String> listFriends = currentUser.getList("friendsId");
                    if (listFriends!=null && listFriends.size()>0) {
                        ParseQuery<ParseUser> query = ParseUser.getQuery();
                        List<String> friendsList = currentUser.getList("friendsId");
                        if (friendsList.isEmpty()){
                            List<ParseUser> list = new ArrayList<ParseUser>();
                            ParseUser dummy = new ParseUser();
                            dummy.setUsername("no friends");
                            list.add(new ParseUser());
                            return list;
                        }
                        query.whereContainedIn("objectId", friendsList);
                        try {
                            friends = query.find();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    }
                }
                return friends;
            }

            @Override
            protected void onPostExecute(Object friends) {
                callback.run(friends);
                super.onPostExecute(friends);
            }
        }
        Async task = new Async();
        task.execute();
    }

    public static void users(final String username,final PgetterCallback callback){
        class Async extends AsyncTask<Object,Object,Object>{
            @Override
            protected Object doInBackground(Object... params) {
                ParseUser currentUser = ParseUser.getCurrentUser();
                List<ParseUser> users = null;
                if (currentUser!=null){
                    try {
                        currentUser.fetch();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    List<String> listfriends = currentUser.getList("friendsId");
                    if(listfriends!=null  && listfriends.size()>0){
                        query.whereNotContainedIn("objectId", listfriends);
                    }

                    query.whereContains("username", username);
                    try {
                        users= query.find();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                return users;
            }

            @Override
            protected void onPostExecute(Object users) {
                callback.run(users);
                super.onPostExecute(users);
            }
        }
        Async task = new Async();
        task.execute();
    }


    //misc
    public interface PgetterCallback {
        void run(Object object);
    }
}

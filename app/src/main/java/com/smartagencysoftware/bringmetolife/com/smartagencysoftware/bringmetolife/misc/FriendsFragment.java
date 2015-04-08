package com.smartagencysoftware.bringmetolife.com.smartagencysoftware.bringmetolife.misc;

/**
 * Created by Alexey on 07.04.2015.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.parse.ParseUser;
import com.smartagencysoftware.bringmetolife.R;
import com.smartagencysoftware.bringmetolife.parse.Pgetter;

import java.util.ArrayList;
import java.util.List;

import static android.R.layout.simple_list_item_1;

/**
 * A placeholder fragment containing a simple view.
 */
public class FriendsFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private ListView mFriendsList;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static FriendsFragment newInstance(int sectionNumber) {
        FriendsFragment fragment = new FriendsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public FriendsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_friends, container, false);
        mFriendsList = (ListView) rootView.findViewById(R.id.listFriends);
        return rootView;
    }

    @Override
    public void onResume() {
        Pgetter.friends(new getFriendsCallback(getActivity()));
        super.onResume();
    }

    //misc
    public  class getFriendsCallback implements Pgetter.PgetterCallback{
        private Context context;

        getFriendsCallback(Context context){
            this.context = context;
        }

        @Override
        public void run(Object object) {
            List<ParseUser> list = (List<ParseUser>) object;
            if (list==null){
                list = new ArrayList<ParseUser>();
            }
            if(list.isEmpty()){
                ParseUser dummy = new ParseUser();
                dummy.setUsername("No friends");
                list.add(dummy);
            }


            String[] users = new String[list.size()];
            for (int i=0; i<list.size();i++){
                users[i] = list.get(i).getUsername();
            }
            ListAdapter adapter = new ArrayAdapter<String>(context, simple_list_item_1,users);
            mFriendsList.setAdapter(adapter);
        }
    }
}
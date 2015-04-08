package com.smartagencysoftware.bringmetolife.com.smartagencysoftware.bringmetolife.misc;

/**
 * Created by Alexey on 07.04.2015.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.smartagencysoftware.bringmetolife.R;
import com.smartagencysoftware.bringmetolife.parse.Pgetter;
import com.smartagencysoftware.bringmetolife.parse.Psetter;

import java.util.ArrayList;
import java.util.List;

import static android.R.layout.simple_list_item_1;

/**
 * A placeholder fragment containing a simple view.
 */
public class UsersFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private ListView mUsersList;
    private Button mSearchButton;
    private EditText mEditText;
    private List<ParseUser> mParseUserList;
    ProgressBar mUsersProgressBar;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static UsersFragment newInstance(int sectionNumber) {
        UsersFragment fragment = new UsersFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public UsersFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_users, container, false);
        mUsersList = (ListView) rootView.findViewById(R.id.listUsers);
        mSearchButton = (Button) rootView.findViewById(R.id.searchUsersButton);
        mEditText = (EditText)rootView.findViewById(R.id.searchUsername);
        mUsersProgressBar = (ProgressBar) rootView.findViewById(R.id.usersProgressBar);
        mSearchButton.setOnClickListener(new OnClickSearchListener());
        mUsersList.setOnItemClickListener(new OnClickListListener());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    //misc


    private class OnClickSearchListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String username = mEditText.getText().toString();
            mParseUserList = new ArrayList<ParseUser>();
            mUsersProgressBar.setVisibility(View.VISIBLE);
            Pgetter.users(username,new UsersCallback(getActivity()));
        }
    }

    private class OnClickListListener implements AdapterView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ParseUser user = mParseUserList.get(position);
            AlertDialog.Builder alertBuild = new AlertDialog.Builder(getActivity());
            alertBuild.setMessage("Add user "+user.getUsername()+" to your friends? ")
                      .setPositiveButton("OK", new OnPositiveClick(user.getObjectId()))
                      .setNegativeButton("cancel", new OnNegativeClick());
            alertBuild.create().show();
        }
    }

    private class OnPositiveClick implements DialogInterface.OnClickListener{
        private String userId;
        OnPositiveClick(String userId){
            this.userId = userId;
        }
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Psetter.plusFriend(userId,new PlusFriendCallback());
            dialog.cancel();
        }
    }

    private class OnNegativeClick implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }
    }

    private class UsersCallback implements Pgetter.PgetterCallback{

        private Context context;
        UsersCallback(Context context){
            this.context = context;
        }
        @Override
        public void run(Object object) {
            mUsersProgressBar.setVisibility(View.INVISIBLE);
            mParseUserList = (List<ParseUser>) object;
            if (mParseUserList.isEmpty()){
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setPositiveButton("No user with such name", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                return;
            }
            String[] users = new String[mParseUserList.size()];
            for (int i=0; i< mParseUserList.size();i++){
                users[i] = mParseUserList.get(i).getUsername();
            }
            ListAdapter adapter = new ArrayAdapter<String>(context, simple_list_item_1,users);
            mUsersList.setAdapter(adapter);
        }
    }

    private class PlusFriendCallback implements Psetter.PsetterCallBack{
        @Override
        public void run(ParseException e) {
            if(e!=null){e.printStackTrace();}
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Friend added!").setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
            }
        }
    }
}
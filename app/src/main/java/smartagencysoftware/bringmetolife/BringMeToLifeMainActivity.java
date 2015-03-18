package smartagencysoftware.bringmetolife;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseCrashReporting;
import com.parse.ParseUser;

import smartagencysoftware.bringmetolife.smartagencysoftware.bringmetolife.service.BringMeToLifeService;


public class BringMeToLifeMainActivity extends ActionBarActivity {

    public static Activity mainActivity;
    static Context context;
    static  Handler uiHandler = new Handler();
    private TextView fullUsername;
    private TextView socialRank;


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
        if(ParseUser.getCurrentUser() == null){ //barely possible with AutomaaticUser enabled
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        fullUsername = (TextView)findViewById(R.id.fullusername);
        socialRank = (TextView)findViewById(R.id.socialrank);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService( new Intent(this, BringMeToLifeService.class));
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (ParseAnonymousUtils.isLinked(currentUser)){
            fullUsername.setText("Anonymous");
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    public static void postInHandler(final String string){
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(context,
                        "foreground app is "+string, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }


}

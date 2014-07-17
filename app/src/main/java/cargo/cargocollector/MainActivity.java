package cargo.cargocollector;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.List;

public class MainActivity extends Activity {

    private TextView mTvStatus;

    private ServiceInitiator initiator;

    private Button mStartButton = null;
    private Button mStopButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Activity", "OnCreate()");
        setContentView(R.layout.activity_start);

        mStartButton = (Button) findViewById(R.id.startservice);
        mStopButton = (Button) findViewById(R.id.stopservice);
        mTvStatus = (TextView) findViewById(R.id.service_status);

        initiator = new ServiceInitiator(MainActivity.this, this);

        initiator.initiateService();

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startService(mIntent);
                initiator.start();
                mStartButton.setEnabled(false);
                mStopButton.setEnabled(true);
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiator.stop();
                mStartButton.setEnabled(true);
                mStopButton.setEnabled(false);
            }
        });
    }

    public void setButtonStatus(boolean start, boolean stop) {
        mStartButton.setEnabled(start);
        mStopButton.setEnabled(stop);
    }




    @Override
    protected void onResume() {
        Log.d("Activity", "OnResume()");
        super.onResume();

        initiator.bind();

    }

    @Override
    protected void onPause() {
        Log.d("Activity", "OnPause()");
        super.onPause();

        //Unbind service.
        initiator.unbind();

    }

    @Override
    protected void onDestroy() {
        Log.d("Activity", "OnDestroy()");
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

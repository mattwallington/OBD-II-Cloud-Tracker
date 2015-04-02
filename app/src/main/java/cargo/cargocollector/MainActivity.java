package cargo.cargocollector;

import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    private Button mStartButton = null;
    private Button mStopButton = null;

    private Context mContext = null;
    private ServiceConnection mConnection = null;
    private CollectorService mBoundService = null;
    private boolean mIsBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        Log.d("Activity", "OnCreate()");
        setContentView(R.layout.activity_start);

        mStartButton = (Button) findViewById(R.id.startservice);
        mStopButton = (Button) findViewById(R.id.stopservice);

        initiateCollectorService();

        //Buttons to start/stop background service.
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStartButton.setEnabled(false);
                mStopButton.setEnabled(true);
                startCollectorService();
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStartButton.setEnabled(true);
                mStopButton.setEnabled(false);
                mBoundService.onDestroy();
            }
        });

    }

    private void initiateCollectorService() {
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mBoundService = ((CollectorService.CollectorBinder) service).getService();

                Log.d("CollectorService", "Service connected." + className.toString());
                mIsBound = true;

                //Set button status
                setButtonStatus(!mBoundService.isRunning(), mBoundService.isRunning());
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.d("CollectorService", "Service disconnected");
                mBoundService = null;
                mIsBound = false;

                //Set button status
                setButtonStatus(!mBoundService.isRunning(), mBoundService.isRunning());
            }
        };
    }

    private void startCollectorService() {
        Log.d("CollectorService", "Starting CollectorService.");
        startService(new Intent(mContext, CollectorService.class));
    }

    /*
     *  Toggle button enabled.
     */
    public void setButtonStatus(boolean start, boolean stop) {
        mStartButton.setEnabled(start);
        mStopButton.setEnabled(stop);
    }

    @Override
    protected void onResume() {
        Log.d("Activity", "OnResume()");
        super.onResume();

        //Bind to background service.
        if (!mIsBound) {
            bindService(new Intent(mContext, CollectorService.class), mConnection, Context.BIND_AUTO_CREATE);
            mIsBound = true;
            Log.d("CollectorService", "CollectorService bound.");
        }


    }

    @Override
    protected void onPause() {
        Log.d("Activity", "OnPause()");
        super.onPause();

        //Unbind service.
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
            Log.d("CollectorService", "CollectorService unbound.");
        }

    }

    @Override
    protected void onDestroy() {
        Log.d("Activity", "OnDestroy()");
        super.onDestroy();

        if (mIsBound) {
            mBoundService.onDestroy();
            Log.d("CollectorService", "Calling CollectorService.onDestroy()");
        }

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

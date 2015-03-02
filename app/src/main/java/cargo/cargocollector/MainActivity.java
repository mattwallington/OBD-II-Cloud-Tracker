package cargo.cargocollector;

import android.app.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    private ServiceInitiator mInitiator;
    private Button mStartButton = null;
    private Button mStopButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Activity", "OnCreate()");
        setContentView(R.layout.activity_start);

        mStartButton = (Button) findViewById(R.id.startservice);
        mStopButton = (Button) findViewById(R.id.stopservice);

        //Kick off Android service.
        mInitiator = new ServiceInitiator(this, MainActivity.this);
        mInitiator.initiateService();

        //Buttons to start/stop background service.
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInitiator.start();
                mStartButton.setEnabled(false);
                mStopButton.setEnabled(true);
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInitiator.stop();
                mStartButton.setEnabled(true);
                mStopButton.setEnabled(false);
            }
        });

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
        if (!mInitiator.isBound())
            mInitiator.bind();

    }

    @Override
    protected void onPause() {
        Log.d("Activity", "OnPause()");
        super.onPause();

        //Unbind service.
        if (mInitiator.isBound())
            mInitiator.unbind();
    }

    @Override
    protected void onDestroy() {
        Log.d("Activity", "OnDestroy()");
        super.onDestroy();

        if (mInitiator.isBound())
            mInitiator.unbind();
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

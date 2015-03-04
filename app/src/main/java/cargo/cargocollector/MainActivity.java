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
    private Button m_startButton = null;
    private Button m_stopButton = null;

    private Context m_context = null;
    private ServiceConnection m_connection = null;
    private CollectorService m_boundService = null;
    private boolean m_isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_context = this;
        Log.d("Activity", "OnCreate()");
        setContentView(R.layout.activity_start);

        m_startButton = (Button) findViewById(R.id.startservice);
        m_stopButton = (Button) findViewById(R.id.stopservice);

        initiateCollectorService();

        //Buttons to start/stop background service.
        m_startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_startButton.setEnabled(false);
                m_stopButton.setEnabled(true);
                startCollectorService();
            }
        });

        m_stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_startButton.setEnabled(true);
                m_stopButton.setEnabled(false);
                m_boundService.onDestroy();
            }
        });

    }

    private void initiateCollectorService() {
        m_connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                m_boundService = ((CollectorService.CollectorBinder) service).getService();

                Log.d("CollectorService", "Service connected." + className.toString());
                m_isBound = true;

                //Set button status
                setButtonStatus(!m_boundService.isRunning(), m_boundService.isRunning());
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.d("CollectorService", "Service disconnected");
                m_boundService = null;
                m_isBound = false;

                //Set button status
                setButtonStatus(!m_boundService.isRunning(), m_boundService.isRunning());
            }
        };
    }

    private void startCollectorService() {
        Log.d("CollectorService", "Starting CollectorService.");
        startService(new Intent(m_context, CollectorService.class));
    }

    /*
     *  Toggle button enabled.
     */
    public void setButtonStatus(boolean start, boolean stop) {
        m_startButton.setEnabled(start);
        m_stopButton.setEnabled(stop);
    }

    @Override
    protected void onResume() {
        Log.d("Activity", "OnResume()");
        super.onResume();

        //Bind to background service.
        if (!m_isBound) {
            bindService(new Intent(m_context, CollectorService.class), m_connection, Context.BIND_AUTO_CREATE);
            m_isBound = true;
            Log.d("CollectorService", "CollectorService bound.");
        }


    }

    @Override
    protected void onPause() {
        Log.d("Activity", "OnPause()");
        super.onPause();

        //Unbind service.
        if (m_isBound) {
            unbindService(m_connection);
            m_isBound = false;
            Log.d("CollectorService", "CollectorService unbound.");
        }

    }

    @Override
    protected void onDestroy() {
        Log.d("Activity", "OnDestroy()");
        super.onDestroy();

        if (m_isBound) {
            m_boundService.onDestroy();
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

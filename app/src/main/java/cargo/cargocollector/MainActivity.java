package cargo.cargocollector;

import android.app.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    private ServiceInitiator m_initiator;
    private Button m_startButton = null;
    private Button m_stopButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Activity", "OnCreate()");
        setContentView(R.layout.activity_start);

        m_startButton = (Button) findViewById(R.id.startservice);
        m_stopButton = (Button) findViewById(R.id.stopservice);

        //Kick off Android service.
        m_initiator = new ServiceInitiator(this, MainActivity.this);
        m_initiator.initiateService();

        //Buttons to start/stop background service.
        m_startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_initiator.start();
                m_startButton.setEnabled(false);
                m_stopButton.setEnabled(true);
            }
        });

        m_stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_initiator.stop();
                m_startButton.setEnabled(true);
                m_stopButton.setEnabled(false);
            }
        });

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
        if (!m_initiator.isBound())
            m_initiator.bind();

    }

    @Override
    protected void onPause() {
        Log.d("Activity", "OnPause()");
        super.onPause();

        //Unbind service.
        if (m_initiator.isBound())
            m_initiator.unbind();
    }

    @Override
    protected void onDestroy() {
        Log.d("Activity", "OnDestroy()");
        super.onDestroy();

        if (m_initiator.isBound())
            m_initiator.unbind();
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

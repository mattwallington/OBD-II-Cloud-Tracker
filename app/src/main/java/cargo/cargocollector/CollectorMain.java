package cargo.cargocollector;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONObject;

public class CollectorMain extends Activity {

    private TextView tv;

    ObdService obdService = null;
    LocationService locationService = null;
    SensorService sensorService = null;

    /* Socket */
    //private static final String SOCKET_URL = "http://10.1.10.12:3232/";
    private static final String SOCKET_URL = "http://demo.cargo.ai:3232/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        tv = (TextView) findViewById(R.id.service_status);

        //Activate location tracking.
        locationService = new LocationService((LocationManager) getSystemService(Context.LOCATION_SERVICE));

        //Activate Accelerometer
        sensorService = new SensorService((SensorManager)getSystemService(SENSOR_SERVICE));

        //Activate OBD service
        obdService = new ObdService();

        //Connect to socket.
        SocketIOService server = new SocketIOService(SOCKET_URL);

        JSONObject obj = new JSONObject();
        try {
            obj.put("user", "user");
            obj.put("pass", "pass");
            obj.put("test", "testdata");
        } catch (Exception e) {
            Log.d("Exception", e.getMessage());
        }
        server.sendData(obj);
    }

    @Override
    protected void onResume() {
        Log.d("Service", "OnResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d("Service", "OnPause()");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d("Service", "OnDestroy()");
        obdService.stopThreads();
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

package cargo.cargocollector;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.support.v4.content.LocalBroadcastManager;

import java.util.UUID;

import cargo.cargocollector.R;

public class StartActivity extends Activity implements LocationListener, SensorEventListener {

    private TextView tv;

    /*  Location */
    private LocationManager locationManager;
    private String provider;
    private static final int MIN_DISTANCE = 1;
    private static final int MIN_TIME = 1000;

    /* Accelerometer */
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Float accel_x, accel_y, accel_z;

    /* Bluetooth / OBD2 */
    private BluetoothAdapter btadapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        //Activate location tracking.
        activateLocation();

        //Activate Accelerometer
        activateAccelerometer();
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

    /*
     *   Location services
     */
    private void activateLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //Find out which provider is best to use.  (GPS or network).
        setBestProvider();

        //Get initial location.  May be invalid.
        Location location = locationManager.getLastKnownLocation(provider);

        //Initialize location fields.
        if (location != null) {
            onLocationChanged(location);
        } else {
            Log.d("Location", "location is null");
        }

        locationManager.requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        //deal with location.
        String loc = Double.toString(location.getLatitude()) + ", " + Double.toString(location.getLongitude());
        tv.append(loc + "\n");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //Not sure what status this is talking about.

    }

    @Override
    public void onProviderEnabled(String provider) {
        setBestProvider();
    }

    @Override
    public void onProviderDisabled(String provider) {
        setBestProvider();
    }

    private void setBestProvider() {
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);
        Log.d("Location", "Location Provider: " + provider);
    }

    /*
     *   Accelerometer services
     */
    private void activateAccelerometer() {
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);


    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        accel_x = event.values[0];
        accel_y = event.values[1];
        accel_z = event.values[2];

        //TODO: Do something here with the accelerometer data.

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*
     *  Bluetooth / OBD2
     */
    private void getOBD() {
        //Get bluetooth adapter
        btadapter = BluetoothAdapter.getDefaultAdapter();
        if (btadapter == null) {
            //Couldn't get adapter.
        }

        if (!btadapter.isEnabled()) {
            Log.d("OBD", "Bluetooth is not enabled");
            return;
        }

        final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    }
}

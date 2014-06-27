package cargo.cargocollector;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

import java.util.UUID;


/**
 * Created by matt on 6/26/14.
 */
public class CargoCollector extends Service implements LocationListener, SensorEventListener {

    /*  Location */
    private LocationManager locationManager;
    private String provider;
    public static final String ACTION_LOCATION_BROADCAST = CargoCollector.class.getName() + "LocationBroadcast";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    private static final int MIN_DISTANCE = 1;
    private static final int MIN_TIME = 1000;

    /* Accelerometer */
    public static final String ACTION_ACCELEROMETER_BROADCAST = CargoCollector.class.getName() + "AccelerometerBroadcast";
    public static final String EXTRA_ACCELEROMETER_X = "extra_accelerometer_x";
    public static final String EXTRA_ACCELEROMETER_Y = "extra_accelerometer_y";
    public static final String EXTRA_ACCELEROMETER_Z = "extra_accelerometer_z";
    private SensorManager sensorManager;
    private Sensor accelerometer;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //Activate location tracking.
        activateLocation();

        //Activate Accelerometer
        activateAccelerometer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        returnCoords(location);
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

    private void returnCoords(Location location) {
        if (location != null) {
            Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
            intent.putExtra(EXTRA_LATITUDE, location.getLatitude());
            intent.putExtra(EXTRA_LONGITUDE, location.getLongitude());
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
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
        Intent intent = new Intent(ACTION_ACCELEROMETER_BROADCAST);
        intent.putExtra(EXTRA_ACCELEROMETER_X, event.values[0]);
        intent.putExtra(EXTRA_ACCELEROMETER_Y, event.values[1]);
        intent.putExtra(EXTRA_ACCELEROMETER_Z, event.values[2]);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*
     *  Bluetooth / OBD2
     */
    private void getOBD() {
        //Get bluetooth adapter
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            //Couldn't get adapter.
        }

        if (!adapter.isEnabled()) {
            Log.d("OBD", "Bluetooth is not enabled");
            return;
        }

        final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    }
}

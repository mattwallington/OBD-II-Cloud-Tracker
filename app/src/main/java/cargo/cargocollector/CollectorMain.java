package cargo.cargocollector;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import java.nio.charset.Charset;

import cargo.cargocollector.R;

public class CollectorMain extends Activity implements LocationListener, SensorEventListener {

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
    private BluetoothSocket btsocket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothThread btinst;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        tv = (TextView) findViewById(R.id.service_status);

        //Activate location tracking.
        activateLocation();

        //Activate Accelerometer
        activateAccelerometer();

        //Activate OBD
        getOBD();

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
        try {
            String point = Double.toString(location.getLatitude()) + ", " + Double.toString(location.getLongitude());
            tv.append(point + "\n");
            Log.d("Location", point);
            //TODO: Do something with location data.

        }
        catch (Exception e) {
            Log.d("Exception", e.getMessage());
        }
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
        Log.d("Status", "Going in.");
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d("Status", "Returned");

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
        Log.d("Method", "getOBD()");
        //Get bluetooth adapter
        btadapter = BluetoothAdapter.getDefaultAdapter();
        if (btadapter == null) {
            //Couldn't get adapter.
        }

        if (!btadapter.isEnabled()) {
            Log.d("OBD", "Bluetooth is not enabled");
            return;
        }
        /*
        Set<BluetoothDevice> pairedDevices;
        pairedDevices = btadapter.getBondedDevices();

        for (Iterator<BluetoothDevice> iter = pairedDevices.iterator(); iter.hasNext();) {
            BluetoothDevice device = iter.next();
            Log.d("BluetoothDevice", device.getName() + ": " + device.getAddress() + "; " +);

        }
        */

        //BT Device: "OBDLink MX" 00:04:3E:30:94:66
        BluetoothDevice device = btadapter.getRemoteDevice("00:04:3E:30:94:66");

        try {
            btsocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            //btsocket.connect();
        }
        catch (Exception e) {
            Log.d("Exception", e.getMessage());
        }

        // Read this site:  http://www.codota.com/android/scenarios/51891a46da0a6c5c8ff8531f/android.bluetooth.BluetoothSocket?tag=out_2013_05_05_07_19_34

        // You need to implement an inputstream and outputstream.
        BluetoothThread.Listener listener = new BluetoothThread.Listener() {
            public void onConnected() {

                //Try sending a command.

                try {
                    //Disable echo back.
                    //sendCmd("ATE0\r");
                    //sendCmd("010D\r");
                    /*
                    sendCmd("ATI");
                    Thread.sleep(1000);
                    sendCmd("ATL1");
                    Thread.sleep(1000);
                    sendCmd("ATH1");
                    Thread.sleep(1000);
                    sendCmd("ATS1");
                    Thread.sleep(1000);
                    sendCmd("ATAL");
                    Thread.sleep(1000);
                    sendCmd("ATSP0");
                    Thread.sleep(1000);
                    */
                    sendCmd("010D\r");
                } catch (Exception e) {
                    Log.d("OBD", e.getMessage());
                }

            }
            public void onReceived(byte[] buffer, int length) {
                Log.d("OBD", "onReceived()");
                //Log.d("Data", "Data: "+buffer.toString() + " Length: "+Integer.toString(length));
                try {
                    String data = new String(buffer, "US-ASCII");
                    data = data.trim();
                    //int speed = Integer.parseInt(data, 16);
                    Log.d("Received", data);
                    //Log.d("Converted", Integer.toString(speed));

                } catch (Exception e) {
                    Log.d("Exception", e.getMessage());
                }
            }
            public void onDisconnected() {
                Log.d("OBD", "onDisconnected()");
            }
            public void onError(IOException e) {
                Log.d("OBD", "Error: " + e.getMessage());
            }
        };

        btinst = BluetoothThread.newInstance(btsocket, listener);

    }

    private void sendCmd(String command) {
        try {
            Log.d("Sent", command);
            btinst.write(command.getBytes(Charset.forName("US-ASCII")));
        } catch (IOException e) {
            Log.d("OBD", "IO Exception: " + e.getMessage());
        }


    }

}

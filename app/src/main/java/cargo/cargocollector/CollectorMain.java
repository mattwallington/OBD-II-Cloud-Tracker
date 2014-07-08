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

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONObject;

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
    BluetoothThread btinst;

    /* Socket */
    private static final String SOCKET_URL = "http://10.1.10.12:3232/";
    private Socket socket = null;

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

        connectSocket(SOCKET_URL);
        JSONObject obj = new JSONObject();
        try {
            obj.put("user", "user");
            obj.put("pass", "pass");
            obj.put("test", "testdata");
        } catch (Exception e) {
            Log.d("Exception", e.getMessage());
        }

        sendData(obj);

        socket.disconnect();

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
        /*
        btadapter = BluetoothAdapter.getDefaultAdapter();
        if (btadapter == null) {
            //Couldn't get adapter.
        }

        if (!btadapter.isEnabled()) {
            Log.d("OBD", "Bluetooth is not enabled");
            return;
        }

        Set<BluetoothDevice> pairedDevices;
        pairedDevices = btadapter.getBondedDevices();

        for (Iterator<BluetoothDevice> iter = pairedDevices.iterator(); iter.hasNext();) {
            BluetoothDevice device = iter.next();
            Log.d("BluetoothDevice", device.getName() + ": " + device.getAddress());

        }

        //BT Device: "OBDLink MX" 00:04:3E:30:94:66
        BluetoothDevice device = btadapter.getRemoteDevice("00:04:3E:30:94:66");

        try {
            btsocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            //btsocket.connect();
        }
        catch (Exception e) {
            Log.d("Exception", e.getMessage());
        }
        */

        /*
        BluetoothThread.Listener listener = new BluetoothThread.Listener() {
            public void onConnected() {
                //Try sending a command.
                try {
                    //Disable echo back.
                    //sendOBDCmd("ATE0\r");
                    btinst.queueCommand("ATE0\r");
                    btinst.queueCommand("ATZ1\r");
                    btinst.queueCommand("ATL11\r");
                    btinst.queueCommand("ATH01\r");
                    btinst.queueCommand("ATS01\r");
                    //btinst.queueCommand("ATSP0\r");


                    //Get speed & rpm.
                    for (int c=0; c < 10; c++) {
                        btinst.queueCommand("010D1\r");
                        btinst.queueCommand("010C1\r");
                    }

                } catch (Exception e) {
                    Log.d("OBD", e.getMessage());
                }

            }
            public void onReceived(byte[] buffer, int length) {
                String returnVal = new String();

                try {

                    btinst.setIsBusy(false);

                    String data = new String(buffer, "US-ASCII");
                    data = data.trim();

                    //int speed = Integer.parseInt(data, 16);
                    if (data.length() > 5) {
                        String substr = data.substring(0,5);

                        if (substr.equals("41 0C")){    //RPM
                            if (data.length() > 7) {
                                int rpm = Integer.parseInt(data.substring(6).replaceAll("[^0-9A-F]" ,""), 16);
                                Log.d("RPM", Integer.toString(rpm));
                            }
                        }
                        else if (substr.equals("41 0D")) {  //Speed
                            if (data.length() > 7) {
                                int speed = Integer.parseInt(data.substring(6).replaceAll("[^0-9a-f]" ,""), 16);
                                Log.d("Speed", Integer.toString(speed));
                            }
                        }
                    }
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
        */

        //btinst = BluetoothThread.newInstance(btsocket, listener);
        btinst = new BluetoothThread();
        btinst.start();
        btinst.startQueueProcessor();

        //Connecting message.
        for (int c=30; c>0; c--) {
            Log.d("ConnectionState", "Connecting: " + Integer.toString(c));
            //Wait for connection.
            if (btinst.isConnected) {
                Log.d("ConnectionState", "Connected");
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.d("Exception", e.getMessage());
            }
        }

        btinst.loopCommands();

    }

    private void connectSocket(String socket_url) {
        try {
            socket = IO.socket(socket_url);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    //socket.emit("foo", "hi");
                    //socket.disconnect();
                }

            }).on("event", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                }

            });
            socket.connect();
        } catch (Exception e) {
            Log.d("Exception", e.getMessage());
        }
    }

    private void sendData(JSONObject obj) {

        //JSONObject obj = new JSONObject();
        socket.emit("data", obj);

    }
}

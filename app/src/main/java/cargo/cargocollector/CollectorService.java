package cargo.cargocollector;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

public class CollectorService extends Service {
    private final IBinder mBinder = new CollectorBinder();

    ObdService obdService = null;
    LocationService locationService = null;
    SensorService sensorService = null;

    private boolean isRunning = false;

    /* Socket */
    //private static final String SOCKET_URL = "http://10.1.10.12:3232/";
    private static final String SOCKET_URL = "http://demo.cargo.ai:3232/";


    public CollectorService() {

    }

    public class CollectorBinder extends Binder {
        CollectorService getService() {
            return CollectorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        // TODO: Return the communication channel to the service.
        return mBinder;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Created
        Log.d("Service", "onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service", "onStartCommand()");
        Log.d("Service", "Starting service with start ID: " + startId + ": " + intent);

        //Set service status.
        isRunning = true;

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

        //Continue running until we stop the service.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Destroyed service.
        Log.d("Service", "OnDestroy()");

        //Destroy location service.
        try {
            locationService.cancel();
        } catch (Exception locexcept) {
            Log.d("Location", "Exception: " + locexcept.getMessage());
        }

        //Destroy sensor service.
        try {
            sensorService.cancel();
        } catch (Exception sensorexcept) {
            Log.d("Sensor", "Exception: " + sensorexcept.getMessage());
        }

        //Destroy OBD exceptions.
        try {
            obdService.cancel();
        } catch (Exception obdexcept) {
            Log.d("OBD", "Exception: " + obdexcept.getMessage());
        }

        //Set service status
        isRunning = false;
    }

    public boolean getIsRunning() {
        return isRunning;
    }

}

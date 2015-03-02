package cargo.cargocollector;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class CollectorService extends Service {
    private final IBinder mBinder = new CollectorBinder();

    private LocationService mLocationService = null;
    private SensorService mSensorService = null;

    private boolean mIsRunning = false;

    /* Socket */
    /*private static final String SOCKET_URL = "http://srv.cargo.ai:31337/";*/

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

    /*
     * Start each of the collection services.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service", "onStartCommand()");
        Log.d("Service", "Starting service with start ID: " + startId + ": " + intent);

        //Set service status.
        mIsRunning = true;

        /*
        //Connect to socket.
        SocketIOService server = new SocketIOService(SOCKET_URL);
        */

/*
        String json = "{\"timestamp\":1424765173,\"location\":{\"timestamp\":1424765173,\"lat\":34.4300034,\"lng\":2392030909239},\"engine_temp\":202.1,\"speed\":23.5,\"running\":1,\"mpg\":25}";
        ZmqClient.connect(json);
        //ZmqClient.send(json);
*/

        //Activate location tracking service
        mLocationService = new LocationService((LocationManager) getSystemService(Context.LOCATION_SERVICE));
        mLocationService.start();

        //Activate Accelerometer service
        //mSensorService = new SensorService((SensorManager)getSystemService(SENSOR_SERVICE));

        //Activate OBD service
        //mObdService = new ObdService();
        ObdService.start();


        //Start data aggregator.
        DataAggregator.start();

        //Continue running until we stop the service.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Destroyed service.
        Log.d("Service", "OnDestroy()");

        //Destroy location service.
        try {
            mLocationService.cancel();
        } catch (Exception locexcept) {
            Log.d("Location", "Exception: " + locexcept.getMessage());
        }

        //Destroy sensor service.
        try {
            mSensorService.cancel();
        } catch (Exception sensorexcept) {
            Log.d("Sensor", "Exception: " + sensorexcept.getMessage());
        }

        //Destroy OBD exceptions.
        try {
            ObdService.cancel();
        } catch (Exception obdexcept) {
            Log.d("OBD", "Exception: " + obdexcept.getMessage());
        }

        //Destroy DataAggregator.
        try {
            DataAggregator.cancel();
        } catch (Exception dataaggexcept) {
            Log.d("DataAgg", "Exception: " + dataaggexcept.getMessage());
        }

        //Set service status
        mIsRunning = false;
        Log.d("Service", "Destroying Service");
    }

    public boolean getIsRunning() {
        return mIsRunning;
    }
}

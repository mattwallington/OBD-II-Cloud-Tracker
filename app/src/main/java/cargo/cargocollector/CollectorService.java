package cargo.cargocollector;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class CollectorService extends Service {
    private final IBinder m_binder = new CollectorBinder();

    private LocationService m_locationService = null;
    private SensorService m_sensorService = null;

    private ObdService m_obdService = null;

    private Context m_context = null;

    private boolean m_isRunning = false;

    public class CollectorBinder extends Binder {
        CollectorService getService() {
            return CollectorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return m_binder;
    }

    @Override
    public void onCreate() {
        Log.d("CollectorService", "CollectorService created.");
        m_context = this;
    }

    /*
     * Start each of the collection services.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("CollectorService", "Starting CollectorService with start ID: " + startId + ": " + intent);
        m_isRunning = true;
/*
        String json = "{\"timestamp\":1424765173,\"location\":{\"timestamp\":1424765173,\"lat\":34.4300034,\"lng\":2392030909239},\"engine_temp\":202.1,\"speed\":23.5,\"running\":1,\"mpg\":25}";
        ZmqClient.connect(json);
        //ZmqClient.send(json);
*/
        // Activate location tracking service.
        m_locationService = new LocationService(this);
        m_locationService.start();

        //Activate Accelerometer service
        //m_sensorService = new SensorService((SensorManager)getSystemService(SENSOR_SERVICE));

        //Activate OBD service
        m_obdService = new ObdService(m_context);
        m_obdService.start();


        //Start data aggregator.
        DataAggregator.start();


        //Continue running until we stop the service.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Destroyed service.
        Log.d("Service", "OnDestroy()");

        //Destroy ZMQ Service
        //ZmqClient.cancel();

        //Destroy location service.
        try {
            m_locationService.cancel();
        } catch (Exception e) {
            Log.d("Location", "Exception: onDestroy(): " + e.getMessage());
        }

        //Destroy sensor service.
        try {
            m_sensorService.cancel();
        } catch (Exception e) {
            Log.d("Sensor", "Exception: onDestroy(): " + e.getMessage());
        }

        //Destroy OBD exceptions.
        try {
            m_obdService.stop();
        } catch (Exception e) {
            Log.d("OBD", "Exception: onDestroy(): " + e.getMessage());
        }

        //Destroy DataAggregator.
        try {
            DataAggregator.cancel();
        } catch (Exception e) {
            Log.d("DataAgg", "Exception: onDestroy(): " + e.getMessage());
        }

        //Set service status
        m_isRunning = false;
        Log.d("Service", "Destroying Collector Service");

        super.onDestroy();
    }

    public boolean isRunning() {
        return m_isRunning;
    }
}

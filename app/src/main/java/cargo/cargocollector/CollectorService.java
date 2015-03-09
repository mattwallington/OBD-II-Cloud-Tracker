package cargo.cargocollector;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class CollectorService extends Service {
    private static String TAG;
    private final IBinder mBinder = new CollectorBinder();

    private LocationService mLocationService;
    private SensorService mSensorService;
    private ObdService mObdService;
    private DataAggregator mDataAggregator;
    private ZmqClient mZmqClient;

    private Context mContext;

    private boolean mIsRunning;

    public class CollectorBinder extends Binder {
        CollectorService getService() {
            return CollectorService.this;
        }
    }

    public CollectorService() {
        TAG = this.getClass().getSimpleName();
        mLocationService = null;
        mSensorService = null;
        mObdService = null;
        mDataAggregator = null;
        mContext = null;
        mZmqClient = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "CollectorService created.");
        mContext = this;
    }

    /*
     * Start each of the collection services.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting CollectorService with start ID: " + startId + ": " + intent);
        mIsRunning = true;

        // Activate location tracking service.
        mLocationService = new LocationService(mContext);
        mLocationService.start();

        //Activate Accelerometer service
        //mSensorService = new SensorService((SensorManager)getSystemService(SENSOR_SERVICE));

        //Activate OBD service
        mObdService = new ObdService(mContext);
        mObdService.start();

        // ZMQ Library.
        mZmqClient = new ZmqClient(mContext);
        mZmqClient.start();

        //Start data aggregator.
        mDataAggregator = new DataAggregator(mZmqClient);
        mDataAggregator.start();

        //Continue running until we stop the service.
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        //Destroyed service.
        Log.d(TAG, "OnDestroy()");


        //Destroy location service.
        try {
            if (mLocationService != null) {
                mLocationService.cancel();
                mLocationService = null;
            }
        } catch (Exception e) {
            Log.e("Location", "Exception: onDestroy(): ", e);
        }

        //Destroy sensor service.
        try {
            if (mSensorService != null) {
                mSensorService.cancel();
                mSensorService = null;
            }
        } catch (Exception e) {
            Log.e("Sensor", "Exception: onDestroy(): ", e);
        }

        //Destroy OBD exceptions.
        try {
            if (mObdService != null){
                mObdService.cancel();
                mObdService = null;
            }
        } catch (Exception e) {
            Log.e("OBD", "Exception: onDestroy(): ", e);
        }

        //Destroy DataAggregator.
        try {
            if (mDataAggregator != null) {
                mDataAggregator.cancel();
                mDataAggregator = null;
            }
        } catch (Exception e) {
            Log.e("DataAgg", "Exception: onDestroy(): ", e);
        }

        //Destroy ZMQ Client.
        try {
            if (mZmqClient != null) {
                mZmqClient.cancel();
                mZmqClient = null;
            }
        } catch (Exception e) {
            Log.e("ZMQClient", "Exception cancelling ZMQ service.", e);
        }

        //Set service status
        mIsRunning = false;
        Log.d(TAG, "Destroying Collector Service");

        super.onDestroy();
    }

    public boolean isRunning() {
        return mIsRunning;
    }
}

package cargo.cargocollector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.json.JSONObject;

/**
 * Created by matt on 7/8/14.
 * Read data from accelerometer.
 */
public class SensorService implements SensorEventListener {
    private SensorManager mSensorManager = null;
    private Sensor mAccelerometer = null;
    private float mAccelX, mAccelY, mAccelZ;

    /*
     * Constructor.  Start accelerometer service.
     */
    public SensorService(SensorManager sensorManager) {
        this.mSensorManager = sensorManager;
        activateAccelerometer();

    }

    /*
     *   Accelerometer services
     */
    private void activateAccelerometer() {
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /*
     * Event handler for accelerometer data changing.
     */
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        mAccelX = event.values[0];
        mAccelY = event.values[1];
        mAccelZ = event.values[2];

        JSONObject obj = new JSONObject();

        try {
            obj.put("ACCEL_X", mAccelX);
            obj.put("ACCEL_Y", mAccelY);
            obj.put("ACCEL_Z", mAccelZ);
        } catch (Exception e) {
            Log.d("Sensor", "Exception: " + e.getMessage());
        }
        // TODO: Send Accel data to Data Aggregator

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*
     * Stop listening to the accelerometer service.
     */
    public void cancel() {
        Log.d("Sensor", "Stopping sensor listener.");
        mSensorManager.unregisterListener(this);
    }

}

package cargo.cargocollector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by matt on 7/8/14.
 */
public class SensorService implements SensorEventListener {
    private SensorManager mSensorManager = null;
    private Sensor mAccelerometer = null;
    private float mAccel_x;
    private float mAccel_y;
    private float mAccel_z;

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

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        mAccel_x = event.values[0];
        mAccel_y = event.values[1];
        mAccel_z = event.values[2];

        //TODO: Do something here with the accelerometer data.

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void cancel() {
        Log.d("Sensor", "Stopping sensor listener.");
        mSensorManager.unregisterListener(this);
    }

}

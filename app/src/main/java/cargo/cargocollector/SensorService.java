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
    private SensorManager sensorManager = null;
    private Sensor accelerometer = null;
    private float accel_x;
    private float accel_y;
    private float accel_z;

    public SensorService(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        activateAccelerometer();
    }

    /*
         *   Accelerometer services
         */
    private void activateAccelerometer() {
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

    public void cancel() {
        Log.d("Sensor", "Stopping sensor listener.");
        sensorManager.unregisterListener(this);
    }

}

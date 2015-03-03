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
    private SensorManager m_sensorManager = null;
    private Sensor m_accelerometer = null;
    private float m_accelX, m_accelY, m_accelZ;

    /*
     * Constructor.  Start accelerometer service.
     */
    public SensorService(SensorManager sensorManager) {
        this.m_sensorManager = sensorManager;
        activateAccelerometer();

    }

    /*
     *   Accelerometer services
     */
    private void activateAccelerometer() {
        m_accelerometer = m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        m_sensorManager.registerListener(this, m_accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /*
     * Event handler for accelerometer data changing.
     */
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        m_accelX = event.values[0];
        m_accelY = event.values[1];
        m_accelZ = event.values[2];

        JSONObject obj = new JSONObject();

        try {
            obj.put("ACCEL_X", m_accelX);
            obj.put("ACCEL_Y", m_accelY);
            obj.put("ACCEL_Z", m_accelZ);
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
        m_sensorManager.unregisterListener(this);
    }

}

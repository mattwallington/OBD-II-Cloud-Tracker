package cargo.cargocollector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.support.v4.content.LocalBroadcastManager;

import cargo.cargocollector.R;

public class StartActivity extends Activity {

    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        tv = (TextView) findViewById(R.id.service_status);
        tv.setText("Service started\n");

        LocalBroadcastManager.getInstance(this).registerReceiver(
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    double latitude = intent.getDoubleExtra(CargoCollector.EXTRA_LATITUDE, 0);
                    double longitude = intent.getDoubleExtra(CargoCollector.EXTRA_LONGITUDE, 0);
                    //Deal with new point.
                    receiveLocationData(latitude, longitude);
                }
            }, new IntentFilter(CargoCollector.ACTION_LOCATION_BROADCAST)
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        float accelerometer_x = intent.getFloatExtra(CargoCollector.EXTRA_ACCELEROMETER_X, 0);
                        float accelerometer_y = intent.getFloatExtra(CargoCollector.EXTRA_ACCELEROMETER_Y, 0);
                        float accelerometer_z = intent.getFloatExtra(CargoCollector.EXTRA_ACCELEROMETER_Z, 0);
                        //Deal with new point.
                        receiveAccelerometerData(accelerometer_x, accelerometer_y, accelerometer_z);
                    }
                }, new IntentFilter(CargoCollector.ACTION_ACCELEROMETER_BROADCAST)
        );

    }

    private void receiveLocationData(Double latitude, Double longitude) {
        tv.append("Lat: " + Double.toString(latitude) + ", Long: " + Double.toString(longitude) + "\n");
    }

    private void receiveAccelerometerData(Float x, Float y, Float z) {
        String sensorData = "Accel_x: " + Float.toString(x) + ", Accel y: " + Float.toString(y) + ", Accel z: " + Float.toString(z) + "\n";
        //tv.append(sensorData);
        //Log.d("Sensor", sensorData);
    }

    @Override
    protected void onResume() {
        Log.d("Service", "OnResume()");
        super.onResume();
        startService(new Intent(this, CargoCollector.class));
    }

    @Override
    protected void onPause() {
        Log.d("Service", "OnPause()");
        super.onPause();
        stopService(new Intent(this, CargoCollector.class));
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
}

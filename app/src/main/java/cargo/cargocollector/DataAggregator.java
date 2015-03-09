package cargo.cargocollector;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.ByteArrayOutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.Deflater;

//Timestamp = System.currentTimeMillis()/1000

/**
 * Aggregate data from all sensors, compress it, and stream it to the cloud.
 */
public class DataAggregator {
    //Static Variables.
    private Timer mTimer;
    private TimerTask mTimerTask;

    private ObjectMapper mMapper;

    //Constants
    private static final int TIMER_PERIOD = 1000;
    private static String TAG;

    private ZmqClient mZmqClient;

    public DataAggregator(ZmqClient zmqClient) {
        TAG = this.getClass().getSimpleName();
        mTimer = null;
        mTimerTask = null;
        mMapper = new ObjectMapper();
        mZmqClient = zmqClient;
    }

    /*
     * Start the timer.
     */
    public void start() {
        mTimer = new Timer();

        initTimerTask();

        mTimer.schedule(mTimerTask, 0, TIMER_PERIOD);
    }

    public void cancel() {
        Log.d("DataAgg", "Stopping Data Aggregator service.");
        stop();
    }

    /*
     * Stop the timer.
     */
    public void stop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void initTimerTask() {
        mTimerTask = new TimerTask() {
            public void run() {         //This will pop every TIMER_PERIOD

                //Copy data to local instance.
                Payload payload = new Payload();

                if (!payload.isData())
                    return;

                Wrapper wrapper = new Wrapper(payload);

                String jsonData = null;
                //Generate JSON (Add timestamp)
                try {
                    jsonData = mMapper.writeValueAsString(wrapper);
                    Log.d("JSON", jsonData);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                byte[] byteJson = null;
                try {
                    byteJson = jsonData.getBytes();
                } catch (Exception e) {
                    Log.d(TAG, "jsonData.getBytes()");
                }
                byte[] byteJsonCompressed = null;


                if (byteJson != null) {
                    //ZIP JSON
                    byteJsonCompressed = compress(byteJson);

                    //Send JSON to server.
                    Log.d(TAG, "Sending: " + jsonData);
                    mZmqClient.send(byteJsonCompressed);
                }
            }
        };

    }

    private byte[] compress(byte[] data) {

        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!deflater.finished()) {
            int byteCount = deflater.deflate(buf);
            baos.write(buf, 0, byteCount);
        }
        deflater.end();

        return baos.toByteArray();
    }
}

@JsonRootName(value="payload")
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
class Payload {       //Copy of data while processing.
    public Location location = null;
    public Float speed = null;
    public Float engine_temp = null;
    public Float mpg = null;
    public String id = "live";
    public Long timestamp = null;

    public Payload() {

        if (Snapshot.location != null) {
            location = new Location();
        }

        if (Snapshot.speed != null)
            speed = convertToMiles(Snapshot.speed);     //convert to MPH

        if (Snapshot.engine_temp != null)
            engine_temp = convertToFahrenheit(Snapshot.engine_temp);

        if (Snapshot.maf != null && Snapshot.speed != null)
            mpg = getMpg(Snapshot.maf, Snapshot.speed);

        //Get timestamp.
        timestamp = System.currentTimeMillis()/1000;

        clearData();
    }

    private float getMpg(float maf, float speed){
        double gph = maf * .0805;
        return (float)(convertToMiles(speed) / gph);
    }

    private float convertToMiles(float kilometers) {
        return (float)(kilometers * 0.621371);
    }

    private float convertToFahrenheit(float celsius) {
        return (float)(celsius * 1.8 + 32);
    }

    private void clearData() {
        Snapshot.speed = null;
        Snapshot.engine_temp = null;
        Snapshot.maf = null;
    }

    @JsonIgnore
    public boolean isData() {
        return (this.location != null || this.speed != null || this.engine_temp != null || this.mpg != null);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");

        result.append(this.getClass().getName() + " Object {" + NEW_LINE);
        if (location != null)
            result.append(" Location: " + location.toString() + NEW_LINE);
        if (speed != null)
            result.append(" Speed: " + speed.toString() + NEW_LINE);
        if (engine_temp != null)
            result.append(" Engine Temp: " + engine_temp.toString() + NEW_LINE);
        if (mpg != null)
            result.append(" MPG: " + mpg.toString() + NEW_LINE);
        result.append("}");
        return result.toString();
    }

}

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonRootName(value="location")
class Location {
    public Long timestamp = null;
    public Double lat = null;
    public Double lng = null;

    public Location() {
        if (Snapshot.location != null) {
            timestamp = Snapshot.location.getTime()/1000;
            lat = Snapshot.location.getLatitude();
            lng = Snapshot.location.getLongitude();
        }
        clearData();
    }

    private void clearData() {
        Snapshot.location = null;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");

        result.append(this.getClass().getName() + " Object {" + NEW_LINE);
        if (timestamp != null)
            result.append(" Timestamp: " + Long.toString(timestamp) + NEW_LINE);
        if (lat != null)
            result.append(" Lat: " + Double.toString(lat) + NEW_LINE);
        if (lng != null)
            result.append(" Lng: " + Double.toString(lng) + NEW_LINE);
        result.append("}");
        return result.toString();
    }
}

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
class Wrapper {
    public String command = "store";
    public Payload payload = null;

    public Wrapper(Payload payload) {
        this.payload = payload;
    }
}
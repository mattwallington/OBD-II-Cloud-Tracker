package cargo.cargocollector;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Timer;
import java.util.TimerTask;

//Timestamp = System.currentTimeMillis()/1000

/**
 * Aggregate data from all sensors, compress it, and stream it to the cloud.
 */
public class DataAggregator {
    //Static Variables.
    private static Timer s_timer;
    private static TimerTask s_timerTask;

    private static ObjectMapper mapper = new ObjectMapper();

    //Constants
    private static final int TIMER_PERIOD = 5000;

    /*
     * Start the timer.
     */
    public static void start() {
        s_timer = new Timer();

        initTimerTask();

        s_timer.schedule(s_timerTask, 0, TIMER_PERIOD);
    }

    public static void cancel() {
        Log.d("DataAgg", "Stopping Data Aggregator service.");
        stop();
    }

    /*
     * Stop the timer.
     */
    public static void stop() {
        if (s_timer != null) {
            s_timer.cancel();
            s_timer = null;
        }
    }

    private static void initTimerTask() {
        s_timerTask = new TimerTask() {
            public void run() {         //This will pop every TIMER_PERIOD

                //Copy data to local instance.
                DataObj data = new DataObj();
                Log.d("Aggregator", "DataObj: " + data.toString());

                //Generate JSON (Add timestamp)
                try {
                    String jsonData = mapper.writeValueAsString(data);
                    Log.d("JSON", jsonData);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //ZIP JSON

                //Send JSON to server.

            }
        };

    }
}

class DataObj {       //Copy of data while processing.
    public Location location = null;
    public Float speed = null;
    public Float engine_temp = null;
    public Float mpg = null;

    public DataObj() {

        if (Snapshot.location != null)
            location = getLocation();

        if (Snapshot.speed != null)
            speed = convertToMiles(Snapshot.speed);     //convert to MPH

        if (Snapshot.engine_temp != null)
            engine_temp = convertToFahrenheit(Snapshot.engine_temp);

        if (Snapshot.maf != null && Snapshot.speed != null)
            mpg = getMpg(Snapshot.maf, Snapshot.speed);

        clearData();
    }

    private Location getLocation(){
        Location loc = new Location();
        loc.timestamp = Snapshot.location.getTime();
        loc.lat = Snapshot.location.getLatitude();
        loc.lng = Snapshot.location.getLongitude();
        return loc;
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
        Snapshot.location = null;
        Snapshot.speed = null;
        Snapshot.engine_temp = null;
        Snapshot.maf = null;
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

class Location {
    Long timestamp = null;
    Double lat = null;
    Double lng = null;

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

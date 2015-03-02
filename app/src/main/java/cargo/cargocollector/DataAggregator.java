package cargo.cargocollector;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;


//Timestamp = System.currentTimeMillis()/1000

/**
 * Aggregate data from all sensors, compress it, and stream it to the cloud.
 */
public class DataAggregator {
    //Member Variables.
    private static Timer mTimer;
    private static TimerTask mTimerTask;
    private static final int TIMER_PERIOD = 5000;

    /*
     * Start the timer.
     */
    public static void start() {
        mTimer = new Timer();

        initTimerTask();

        mTimer.schedule(mTimerTask, 0, TIMER_PERIOD);
    }

    public static void cancel() {
        Log.d("DataAgg", "Stopping Data Aggregator service.");
        stop();
    }

    /*
     * Stop the timer.
     */
    public static void stop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private static void initTimerTask() {
        mTimerTask = new TimerTask() {
            public void run() {         //This will pop every TIMER_PERIOD

                //Copy data to local instance.
                DataObj data = new DataObj();
                Log.d("Aggregator", "DataObj: " + data.toString());

                //Generate JSON (Add timestamp)


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

        if (Data.location != null)
            location = getLocation();

        if (Data.speed != null)
            speed = convertToMiles(Data.speed);     //convert to MPH

        if (Data.engine_temp != null)
            engine_temp = convertToFahrenheit(Data.engine_temp);

        if (Data.maf != null && Data.speed != null)
            mpg = getMpg(Data.maf, Data.speed);

        clearData();
    }

    private Location getLocation(){
        Location loc = new Location();
        loc.timestamp = Data.location.getTime();
        loc.lat = Data.location.getLatitude();
        loc.lng = Data.location.getLongitude();
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
        Data.location = null;
        Data.speed = null;
        Data.engine_temp = null;
        Data.maf = null;
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

package cargo.cargocollector;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by matt on 7/8/14.
 */
public class LocationService implements LocationListener {

    /*  Location */
    private LocationManager mLocationManager;
    private String mProvider;
    private static final int MIN_DISTANCE = 1;
    private static final int MIN_TIME = 1000;
    private SocketIOService mServer;

    public LocationService(LocationManager locationManager, SocketIOService server) {
    //public LocationService(LocationManager locationManager) {
        mServer = server;
        mLocationManager = locationManager;
        activateLocation();
    }

    /*
     *   Location services
     */
    private void activateLocation() {
        //Find out which provider is best to use.  (GPS or network).
        setBestProvider();

        //Get initial location.  May be invalid.
        Location location = mLocationManager.getLastKnownLocation(mProvider);

        //Initialize location fields.
        if (location != null) {
            onLocationChanged(location);
        } else {
            Log.d("Location", "location is null");
        }

        mLocationManager.requestLocationUpdates(mProvider, MIN_TIME, MIN_DISTANCE, this);

    }

    @Override
    public void onLocationChanged(Location location) {
        //deal with location.
        try {
            //String point = Double.toString(location.getLatitude()) + ", " + Double.toString(location.getLongitude());
            //Log.d("Location", point);
            //TODO: Do something with location data.

            Log.d("GPS", "Sending geometry");

            JSONArray latLng = new JSONArray();
            latLng.put(location.getLatitude());
            latLng.put(location.getLongitude());

            JSONObject inner = new JSONObject();
            inner.put("type", "Point");
            inner.put("coordinates", latLng);
            inner.put("timestamp", System.currentTimeMillis()/1000);

            JSONObject outer = new JSONObject();
            outer.put("geometry", inner);

            mServer.sendData(outer);

            //Send test data for alert.
            /*
            {
                "bumped": {
                "timestamp": <unixtime>,
                        "intensity": xxx,
                        "power": 0,
                        "moving": 0
                }
            }
            */
            /*
            inner = new JSONObject();
            inner.put("timestamp", System.currentTimeMillis()/1000);
            inner.put("intensity", 100);
            inner.put("power", 1);
            inner.put("moving", 0);

            outer = new JSONObject();
            outer.put("bumped", inner);

            Log.d("GPS", "Sending bumped: "+outer.toString());

            mServer.sendData(outer);
            */
        }
        catch (Exception e) {
            Log.d("Location", e.getMessage());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //Not sure what status this is talking about.

    }

    @Override
    public void onProviderEnabled(String provider) {
        setBestProvider();
    }

    @Override
    public void onProviderDisabled(String provider) {
        setBestProvider();
    }

    private void setBestProvider() {
        Criteria criteria = new Criteria();
        mProvider = mLocationManager.getBestProvider(criteria, true);
        Log.d("Location", "Location Provider: " + mProvider);
    }

    public void cancel() {
        Log.d("Location", "Cancelling Location service");
        mLocationManager.removeUpdates(this);
    }
}

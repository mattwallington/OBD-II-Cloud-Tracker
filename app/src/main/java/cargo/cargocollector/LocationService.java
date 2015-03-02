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

    /*
     * Constructor.  Activate location service.
     */
    public LocationService(LocationManager locationManager) {
    //public LocationService(LocationManager locationManager) {
        mLocationManager = locationManager;
    }

    /*
     *   Location services
     */
    public void start() {
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

        //Receive location updates whenever location changes.
        mLocationManager.requestLocationUpdates(mProvider, MIN_TIME, MIN_DISTANCE, this);

    }

    @Override
    public void onLocationChanged(Location location) {
        Data.location = location;
        Log.d("Location", "Lat: "+location.getLatitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        setBestProvider();
    }

    @Override
    public void onProviderDisabled(String provider) {
        setBestProvider();
    }

    /*
     * Set location provider based on what's available.  I.E. GPS vs Cell tower triangulation.
     */
    private void setBestProvider() {
        Criteria criteria = new Criteria();
        mProvider = mLocationManager.getBestProvider(criteria, true);
        Log.d("Location", "Location Provider: " + mProvider);
    }

    /*
     * Stop receiving location updates.
     */
    public void cancel() {
        Log.d("Location", "Cancelling Location service");
        mLocationManager.removeUpdates(this);
    }
}
